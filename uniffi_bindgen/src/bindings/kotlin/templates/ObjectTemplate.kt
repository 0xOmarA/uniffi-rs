{% import "macros.kt" as kt %}
{%- let obj = self.inner() %}
public interface {{ obj.name()|class_name }}Interface {
    {% for meth in obj.methods() -%}
    fun {{ meth.name()|fn_name }}({% call kt::arg_list_decl(meth) %})
    {%- match meth.return_type() -%}
    {%- when Some with (return_type) %}: {{ return_type|type_name -}}
    {%- else -%}
    {%- endmatch %}
    {% endfor %}
}

class {{ obj.name()|class_name }}(
    pointer: Pointer
) : FFIObject(pointer), {{ obj.name()|class_name }}Interface {

    {%- match obj.primary_constructor() %}
    {%- when Some with (cons) %}
    constructor({% call kt::arg_list_decl(cons) -%}) :
        this({% call kt::to_ffi_call(cons) %})
    {%- when None %}
    {%- endmatch %}

    /**
     * Disconnect the object from the underlying Rust object.
     *
     * It can be called more than once, but once called, interacting with the object
     * causes an `IllegalStateException`.
     *
     * Clients **must** call this method once done with the object, or cause a memory leak.
     */
    override protected fun freeRustArcPtr() {
        rustCall() { status ->
            _UniFFILib.INSTANCE.{{ obj.ffi_object_free().name() }}(this.pointer, status)
        }
    }

    internal fun lower(): Pointer = callWithPointer { it }

    internal fun write(buf: RustBufferBuilder) {
        // The Rust code always expects pointers written as 8 bytes,
        // and will fail to compile if they don't fit.
        buf.putLong(Pointer.nativeValue(this.lower()))
    }

    {% for meth in obj.methods() -%}
    {%- match meth.return_type() -%}

    {%- when Some with (return_type) -%}
    override fun {{ meth.name()|fn_name }}({% call kt::arg_list_protocol(meth) %}): {{ return_type|type_name }} =
        callWithPointer {
            {%- call kt::to_ffi_call_with_prefix("it", meth) %}
        }.let {
            {{ "it"|lift_var(return_type) }}
        }

    {%- when None -%}
    override fun {{ meth.name()|fn_name }}({% call kt::arg_list_protocol(meth) %}) =
        callWithPointer {
            {%- call kt::to_ffi_call_with_prefix("it", meth) %}
        }
    {% endmatch %}
    {% endfor %}

    companion object {
        internal fun lift(ptr: Pointer): {{ obj.name()|class_name }} {
            return {{ obj.name()|class_name }}(ptr)
        }

        internal fun read(buf: ByteBuffer): {{ obj.name()|class_name }} {
            // The Rust code always writes pointers as 8 bytes, and will
            // fail to compile if they don't fit.
            return {{ obj.name()|class_name }}.lift(Pointer(buf.getLong()))
        }

        {% for cons in obj.alternate_constructors() -%}
        fun {{ cons.name()|fn_name }}({% call kt::arg_list_decl(cons) %}): {{ obj.name()|class_name }} =
            {{ obj.name()|class_name }}({% call kt::to_ffi_call(cons) %})
        {% endfor %}
    }
}
