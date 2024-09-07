config.set({
    concurrency: 4,
    // adjust if necessary - i have no clue which does the trick to prevent karma from signaling browser timeouts
    // during long test runs, i think that the test browser is unresponsive, causing karma to kill it after some time
    // this does not happen when starting karma in debuggin mode though
    browserDisconnectTimeout: 120000,
    browserNoActivityTimeout: 120000,
    captureTimeout: 120000,
    processKillTimeout: 120000,
    pingTimeout: 120000,
    browserSocketTimeout: 120000,
    client: {
        mocha: {
            timeout: 0
        }
    }
});
