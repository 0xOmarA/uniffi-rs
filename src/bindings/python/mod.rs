/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

use std::io::prelude::*;
use std::{
    env,
    collections::HashMap,
    convert::TryFrom, convert::TryInto,
    fs::File,
    iter::IntoIterator,
    fmt::Display,
    path::{Path, PathBuf},
};

use anyhow::bail;
use anyhow::anyhow;
use anyhow::Result;

pub mod gen_python;
pub use gen_python::{Config, PythonWrapper};

use super::super::interface::ComponentInterface;

// Generate python bindings for the given ComponentInterface, as a string.

pub fn generate_python_bindings(ci: &ComponentInterface) -> Result<String> {
    let config = Config::from(&ci);
    use askama::Template;
    PythonWrapper::new(config, &ci).render().map_err(|_| anyhow::anyhow!("failed to render python bindings"))
}

// Generate python bindings for the given ComponentInterface, then

pub fn write_python_bindings(ci: &ComponentInterface, out_dir: &str) -> Result<()> {
    let mut py_file = PathBuf::from(out_dir);
    py_file.push(format!("{}.py", ci.namespace()));
    let mut f = File::create(&py_file).map_err(|e| anyhow!("Failed to create .py file: {:?}", e))?;
    write!(f, "{}", generate_python_bindings(&ci)?).map_err(|e| anyhow!("Failed to write python bindings: {:?}", e))?;
    Ok(())
}

// Execute the specifed kotlin script, with classpath based on the generated
// artifacts in the given output directory.

pub fn run_python_script(out_dir: &str, script_file: Option<&str>) -> Result<()> {
    let mut pythonpath = std::env::var("PYTHONPATH").unwrap_or_else(|_| String::from(""));
    // This lets java find the compiled library for the rust component.
    pythonpath.push_str(":"); pythonpath.push_str(out_dir);
    let mut cmd = std::process::Command::new("python3");
    cmd.env("PYTHONPATH", pythonpath);
    if let Some(script) = script_file {
        cmd.arg(script);
    }
    let status = cmd
        .spawn().map_err(|_| anyhow::anyhow!("failed to spawn `python`"))?
        .wait().map_err(|_| anyhow::anyhow!("failed to wait for `python` subprocess"))?;
    if ! status.success() {
        bail!("running `python` failed")
    }
    Ok(())
}