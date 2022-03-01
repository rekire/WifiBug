# Wifi Bug
A sample Android app to connect to a wifi, which does not work as intended.

If you want to bake in your own wifi without typing it on your mobile you can simple edit your
`local.properties` file and add the keys `defaultSsid` and `defaultPassword`, this will be added as
a string resource and used by default.

This works fine for older devices but simply does not work on my Android 12 device.
