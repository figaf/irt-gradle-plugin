# irt-gradle-plugin
This plugin provides an integration with Figaf IRT.

## Tasks
The plugin has one task `runTestSuite` which actually runs a specified test suite in IRT.

## Configuration
The tasks can be configured through an extension `irtPlugin` which accepts several parameters:
* `deploymentType`* - deployment type of IRT. Possible values are: `on-premise`, `cloud`. Default value: `on-premise`
* `url`* - basic path to the IRT server. Example: `https://app.figaf.com` (for cloud deployment) or `http://localhost:8089/irt` (for on-premise deployment)
* `clientId`* - OAuth 2 client id for authorizing in IRT. Example: `VN9357gt4eJR9Kx`
* `clientSecret`* - OAuth 2 client secret for authorizing in IRT. Example: `tN37gRSsomF47B6n2yZA0E5uVXeGb`
* `testSuiteId` - id of the test suite which will be run. Example: `812fc514-752e-46f7-8843-d6ed4f7c0317`
* `testSuiteName` - name of the test suite which will be run. Example: `My Test Suite`. Used only if `testSuiteId` is not defined.
* `delayBeforePolling` - delay (in milliseconds) between running the test suite and polling the results. In most of case it should be at least grater than 0. Default value: 15000L.
* `synchronizeBeforeRunningTestSuite` - if it is `true` IRT will do a synchronization for the IFlows related to the test suite before running it. Default value: `false`.