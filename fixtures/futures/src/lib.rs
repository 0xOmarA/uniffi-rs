/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

use std::{
    future::Future,
    pin::Pin,
    sync::{Arc, Mutex, MutexGuard},
    task::{Context, Poll, Waker},
    thread,
    time::Duration,
};

/// Non-blocking timer future.
pub struct TimerFuture {
    shared_state: Arc<Mutex<SharedState>>,
}

struct SharedState {
    completed: bool,
    waker: Option<Waker>,
}

impl Future for TimerFuture {
    type Output = ();

    fn poll(self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<Self::Output> {
        let mut shared_state = self.shared_state.lock().unwrap();

        if shared_state.completed {
            Poll::Ready(())
        } else {
            shared_state.waker = Some(cx.waker().clone());
            Poll::Pending
        }
    }
}

impl TimerFuture {
    pub fn new(duration: Duration) -> Self {
        let shared_state = Arc::new(Mutex::new(SharedState {
            completed: false,
            waker: None,
        }));

        let thread_shared_state = shared_state.clone();

        // Let's mimic an event coming from somewhere else, like the system.
        thread::spawn(move || {
            thread::sleep(duration);

            let mut shared_state: MutexGuard<_> = thread_shared_state.lock().unwrap();
            shared_state.completed = true;

            if let Some(waker) = shared_state.waker.take() {
                waker.wake();
            }
        });

        TimerFuture { shared_state }
    }
}

/// Sync function.
#[uniffi::export]
fn greet(who: String) -> String {
    format!("Hello, {who}")
}

/// Async function that is immediatly ready.
#[uniffi::export]
async fn always_ready() -> bool {
    true
}

/// Async function that says something after 2s.
#[uniffi::export]
async fn say() -> String {
    TimerFuture::new(Duration::from_secs(2)).await;

    format!("Hello, Future!")
}

/// Async function that says something after a certain time.
#[uniffi::export]
async fn say_after(secs: u8, who: String) -> String {
    TimerFuture::new(Duration::from_secs(secs.into())).await;

    format!("Hello, {who}!")
}

/// Async function that sleeps!
#[uniffi::export]
pub async fn sleep(secs: u8) -> bool {
    TimerFuture::new(Duration::from_secs(secs.into())).await;

    true
}

/// Sync function that generates a new `Megaphone`.
///
/// It builds a `Megaphone` which has async methods on it.
#[uniffi::export]
fn new_megaphone() -> Arc<Megaphone> {
    Arc::new(Megaphone)
}

/// A megaphone. Be careful with the neighbours.
#[derive(uniffi::Object)]
pub struct Megaphone;

#[uniffi::export]
impl Megaphone {
    /// An async function that yells something after a certain time.
    async fn say_after(self: Arc<Self>, secs: u8, who: String) -> String {
        say_after(secs, who).await.to_uppercase()
    }
}

include!(concat!(env!("OUT_DIR"), "/uniffi_futures.uniffi.rs"));

mod uniffi_types {
    pub(crate) use super::Megaphone;
}
