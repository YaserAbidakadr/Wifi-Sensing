# CRC Indoor WIfi  Sensing

This is an indoor cloud sensing app which using ARCore anchors and send to Cloud Service.

For indoor sensing, the GPS information can't help since the location is very closed.

In this project, when the Cell phone in Sensing stage, when use click on the
view scens, the app will create a sensing points and starting to sense
the Wi-Fi signals. In ARCore termnology, it create an anchor, and send to
cloud.

When the Cell phone in resolve stage, it will search the environment, if it find
an anchor in the scene, it wil show the sensing points history data.

## Getting Started with Cloud Anchor

 See [Get started with Cloud Anchors for Android](https://developers.google.com/ar/develop/java/cloud-anchors/cloud-anchors-quickstart-android)
 to learn how to set up your development environment and try out this sample app.

# Developing stage

## Limitation
The cloud anchor period
-   Only 24 hours
-   Waiting for the next version

## Generate the project from the Cloud Anchor Example
-   Done

## Change to support multi anchors

### Allow multi cloud anchor

### add support for the

### add suport for the clear and resolve

# Make it an project so that we can do following
- add mqtt support so that the anchors can be share in the flying
- add the wifi sensening moduel and send to the mqtt server
- add the view module after you click the anchor, it will ask the cloud to get the sensor data

# Todo List

## In app/build.gradle

- if I change the appid to myself name, it wont build
The error is:
```
    Build Failed: No matching client found for package name 'ca.gc.crc.rnad.indoorwifi'
```

So I leave it as is.

- I believe it is relative with the application id, I will change it later in the google concose

