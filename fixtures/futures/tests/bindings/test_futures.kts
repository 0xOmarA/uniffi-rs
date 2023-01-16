import uniffi.fixture.futures.*
import kotlinx.coroutines.*
import kotlin.system.*

// init UniFFI to get good measurements after that
runBlocking {
    val time = measureTimeMillis {
        alwaysReady()
    }

    println("init time: ${time}ms")
}

// Test `always_ready`.
runBlocking {
    val time = measureTimeMillis {
        val result = alwaysReady()

        assert(result == true)
    }

    print("always_ready: ${time}ms")
    assert(time <= 4)
    println(" ... ok")
}

// Test `void`.
runBlocking {
    val time = measureTimeMillis {
        val result = void()

        assert(result == Unit)
    }

    print("void: ${time}ms")
    assert(time <= 4)
    println(" ... ok")
}

// Test `sleep`.
runBlocking {
    val time = measureTimeMillis {
        sleep(2U)
    }

    print("sleep: ${time}ms")
    assert(time > 2000 && time < 2100)
    println(" ... ok")
}

// Test sequential futures.
runBlocking {
    val time = measureTimeMillis {
        val resultAlice = sayAfter(1U, "Alice")
        val resultBob = sayAfter(2U, "Bob")

        assert(resultAlice == "Hello, Alice!")
        assert(resultBob == "Hello, Bob!")
    }

    print("sequential futures: ${time}ms")
    assert(time > 3000 && time < 3100)
    println(" ... ok")
}

// Test concurrent futures.
runBlocking {
    val time = measureTimeMillis {
        val resultAlice = async { sayAfter(1U, "Alice") }
        val resultBob = async { sayAfter(2U, "Bob") }

        assert(resultAlice.await() == "Hello, Alice!")
        assert(resultBob.await() == "Hello, Bob!")
    }

    print("concurrent futures: ${time}ms")
    assert(time > 2000 && time < 2100)
    println(" ... ok")
}

// Test async methods.
runBlocking {
    val megaphone = newMegaphone()
    val time = measureTimeMillis {
        val resultAlice = megaphone.sayAfter(2U, "Alice")

        assert(resultAlice == "HELLO, ALICE!")
    }

    print("async methods: ${time}ms")
    assert(time > 2000 && time < 2100)
    println(" ... ok")
}

// Test with the Tokio runtime.
runBlocking {
    val time = measureTimeMillis {
        val resultAlice = sayAfterWithTokio(2U, "Alice")

        assert(resultAlice == "Hello, Alice (with Tokio)!")
    }

    print("with tokio runtime: ${time}ms")
    assert(time > 2000 && time < 2100)
    println(" ... ok")
}

// Test fallible function/method.
runBlocking {
    val time1 = measureTimeMillis {
        try {
            val result = fallibleMe(false)
            assert(true)
        } catch (exception: Exception) {
            assert(false) // should never be reached
        }
    }

    print("fallible function (with result): ${time1}ms")
    assert(time1 < 10)
    println(" ... ok")

    val time2 = measureTimeMillis {
        try {
            val result = fallibleMe(true)
            assert(false) // should never be reached
        } catch (exception: Exception) {
            assert(true)
        }
    }

    print("fallible function (with exception): ${time2}ms")
    assert(time2 < 60)
    println(" ... ok")

    val megaphone = newMegaphone()

    val time3 = measureTimeMillis {
        try {
            val result = megaphone.fallibleMe(false)
            assert(true)
        } catch (exception: Exception) {
            assert(false) // should never be reached
        }
    }

    print("fallible method (with result): ${time3}ms")
    assert(time3 < 10)
    println(" ... ok")

    val time4 = measureTimeMillis {
        try {
            val result = megaphone.fallibleMe(true)
            assert(false) // should never be reached
        } catch (exception: Exception) {
            assert(true)
        }
    }

    print("fallible method (with exception): ${time4}ms")
    assert(time4 < 60)
    println(" ... ok")
}

// Test record.
runBlocking {
    val time = measureTimeMillis {
        val result = newMyRecord("foo", 42U)

        assert(result is MyRecord)
        assert(result.a == "foo")
        assert(result.b == 42U)
    }

    print("record: ${time}ms")
    assert(time < 10)
    println(" ... ok")
}
