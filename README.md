<span id="_Toc456607170" class="anchor"><span id="_Toc456859050" class="anchor"></span></span>iBKS SDK for Android
==================================================================================================================

**ABSTRACT**

The **iBKS SDK** for Android is a library which allows communication, managing and interaction with iBKS Beacons.

The iBKS SDK function prototypes covers the following five packages integrated in the library: 

-	Connection
-	Scan
-	Eddystone Service
-	iBeacon Service
-	Global Service

**AUDIENCE**

The **iBKS SDK** is primarily focused for Android software developers with basic knowledge of beacon configuration


<span id="_Toc456607170" class="anchor"><span id="_Toc456859050" class="anchor"></span></span>Before you start
==============================================================================================================

This SDK will help you to manage iBKS Beacons with your own Android APP
in a few easy steps.

All you need:

-   Android Studio

-   Android device with 5.0 version or above.

-   At least one iBKS Beacon.


<span id="_Toc456607171" class="anchor"><span id="_Toc456859051" class="anchor"></span></span>Let’s play
========================================================================================================

<span id="_Toc456607172" class="anchor"><span id="_Toc456859052" class="anchor"></span></span> 1. Create a project 
------------------------------------------------------------------------------------------------------------------

<span id="_Toc456607173" class="anchor"></span>First of all, create a
new Android Studio project and add the iBKS SDK to the build.gradle
(Module:app) declaring the following dependency:

``compile 'com.accent_systems.ibkslibrary:ibkslibrary:1.0.8'``

The **minSdkVersion** of the app should be **21** or higher because
there are some Bluetooth functions that don’t work for older Android SDK
versions.


2. App permissions 
---------------------------------------------------------------------------------------------------

In order to manage Bluetooth in Android it’s necessary to request some
permissions at user.

### <span id="_Toc455470644" class="anchor"><span id="_Toc456607174" class="anchor"><span id="_Toc456859054" class="anchor"></span></span></span>Location

If the Android version is 6.0 or higher it’s necessary to request
location permission. To do this, it’s necessary to add permission in
AndroidManifest.xml

``<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />``                    
``<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />``

### <span id="_Toc455470645" class="anchor"><span id="_Toc456607175" class="anchor"><span id="_Toc456859055" class="anchor"></span></span></span>Bluetooth

In order To use Bluetooth in Android device, the first thing to do is
check if the device that runs the app has Bluetooth Low Energy (beacons
work with this type of Bluetooth) and if it is enabled. To enable
Bluetooth it’s necessary to add permission in AndroidManifest.xml

``<uses-permission android:name="android.permission.BLUETOOTH" />``         
``<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />``


### Internet

In order to use some functions that request access to internet, it’s
necessary to add permissions on AndroidManifest.xml

``<uses-permission android:name="android.permission.INTERNET" />``


3. EID registration 
------------------------------------------------------

If you would like to use the EID registration in your APP, follow these
steps:

1. Log in to **Google Console Developers **

2. Enable **Proximity Beacon API**

3. Create the **Credentials** for your APP:

-   **Android** **API key**

-   **OAuth Client ID** for your package.

It’s possible that you may wait some minutes or hours to grant the
access to the API.

The users of your APP don’t need to create Credentials but they should
also enable the **Proximity Beacon API** on their account.


4. Get started with a sample project 
------------------------------------

From this github project, you will find the **“iBKS SDK
Sample Project”**. This example shows how to do the main tasks on a
Bluetooth APP such as:

-   Scan devices

-   Connect devices

-   Read/Write characteristics

-   Set/Get Eddystone Slots

-   Register EID beacon

-   Get EID in Clear

-   Set/Get iBeacon Slots

-   Set/Get Characteristics of Global service

-   Get client account and Google project Id

-   Parse advertising data

The **“iBKS SDK Sample Project”** App starts with a scan of bluetooth
devices. When an item of the list is clicked, the app establishes the
connection with the beacon that allows you to do any action. Follow the
next steps to start playing:


1. **Download the project**

2. **Open the project with Android Studio**

3. **Compile the project**

4. **Try the example on your mobile device**

<span id="_Toc456607171" class="anchor"><span id="_Toc456859051" class="anchor"></span></span>
========================================================================================================
Last update: 2016/07/21
