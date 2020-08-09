/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

#[derive(Debug, thiserror::Error)]
enum ArithmeticError {
    #[error("Integer overflow!")]
    IntegerOverflow,
}

fn add(a: u64, b: u64) -> Result<u64> {
    match a.checked_add(b) {
        None => Err(ArithmeticError::IntegerOverflow),
        Some(c) => Ok(c),
    }
}

fn sub(a: u64, b: u64) -> Result<u64> {
    match a.checked_sub(b) {
        None => Err(ArithmeticError::IntegerOverflow),
        Some(c) => Ok(c),
    }
}

fn equal(a: u64, b: u64) -> bool {
    a == b
}

type Result<T, E = ArithmeticError> = std::result::Result<T, E>;

include!(concat!(env!("OUT_DIR"), "/arithmetic.uniffi.rs"));
