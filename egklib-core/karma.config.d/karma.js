config.set({
    // adjust if necessary - i have no clue which does the trick to prevent karma from signaling browser timeouts
    // during long test runs, i think that the test browser is unresponsive, causing karma to kill it after some time
    // this does not happen when starting karma in debug mode though
    browserDisconnectTimeout: 480000,
    browserNoActivityTimeout: 480000,
    captureTimeout: 480000,
    processKillTimeout: 480000,
    pingTimeout: 480000,
    browserSocketTimeout: 240000,
    client: {
        mocha: {
            timeout: 0
        }
    }
});
