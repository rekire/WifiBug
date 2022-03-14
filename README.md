# Wifi Bug
A sample Android app to connect to a wifi, which does not work as intended.

If you want to bake in your own wifi without typing it on your mobile you can simple edit your
`local.properties` file and add the keys `defaultSsid` and `defaultPassword`, this will be added as
a string resource and used by default.

This works fine for older devices but simply does not work on my Android 12 device.

The original code is tagged as [ConnectivityManager].

Right now it seems that the behavior of `ConnectivityManager` is still wrong ([see here for details][bug1]) but
[Google suggest][bug1-dl] to use the `WifiNetworkSuggestion` api, where the [documentation and
behavior seems to be wrong][bug2] in some cases too.

[ConnectivityManager]: https://github.com/rekire/WifiBug/tree/ConnectivityManager
[bug1]: https://issuetracker.google.com/issues/221872199
[bug1-dl]: https://issuetracker.google.com/issues/221872199#comment5
[bug2]: https://issuetracker.google.com/issues/224071894