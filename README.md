[![Build Status](https://travis-ci.org/OpenSRP/opensrp-client-path.svg?branch=master)](https://travis-ci.org/OpenSRP/opensrp-client-path) [![Coverage Status](https://coveralls.io/repos/github/OpenSRP/opensrp-client-path/badge.svg?branch=master)](https://coveralls.io/github/OpenSRP/opensrp-client-path?branch=master)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/4b06c9464e474ae0b2c369fa328c5c91)](https://www.codacy.com/app/OpenSRP/opensrp-client-path?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=OpenSRP/opensrp-client-path&amp;utm_campaign=Badge_Grade)

[![Dristhi](https://raw.githubusercontent.com/OpenSRP/opensrp-client/master/opensrp-app/res/drawable-mdpi/login_logo.png)](https://smartregister.atlassian.net/wiki/dashboard.action)

* [Introduction](#introduction)
   * [Features](#features)
* [Android Studio Setup - (Developer Setup)](#android-studio-setup--developer-setup)
   * [Pre-requisites](#pre-requisites)
   * [Testing Devices](#testing-devices)
   * [Cont... Android Studio Setup](#cont-android-studio-setup)
* [App Walkthrough (End-User)](#app-walkthrough-end-user)
   * Set Test URL (Optional)
   * Login
   * Main activity
      * Create new patient
      * Edit current patient
      * View patient history
      * Search patient
      * [Advanced Search](#advanced-search)
      * [Global Search](#global-search)
      * [Scanning QR Code](#click-on-the-scan-qr-code)
   * [Stock Control](#5-slide-menu)
   * [HIA 2 Reports](#5-slide-menu)
* [Project Dependencies](#project-dependencies)
   * [Folder Structure](#folder-structure)


# Introduction
OpenSRP Path is an app that handles infant records from child birth to 5 years of age. This mainly includes monitoring child weight, vaccination schedules and other supplements that have been proven to prevent diseases and/or improve infant growth and health.

Its main focus is **Infant Health**


## Features

The OpenSRP Path App enables providers to:
 1. Record patient immunisations
 2. Register new patients
 3. Record vaccine stock
 4. Record patient weights over time
 5. Review patient immunisation & weight history
 6. Determine patient health status based on weight
 7. Determine patient health vulnerability based on adherance to immunization schedule
 8. Determine patient health from **above factors**
 9. Record side-effects caused by treatments
 10. Follow-up with patients in the field
 11. Perform stock management of vaccines
 12. Submit HIA 2 forms monthly
 13. Verify HIA 2 forms monthly
 14. Administer to patients from different zones

# Android Studio Setup - (Developer Setup)

## Pre-requisites

1. Make sure you have Java 1.7 to 1.8 installed
2. Make sure you have Android Studio installed or [download it from here](https://developer.android.com/studio/index.html)

## Testing devices

1. Use a physical Android device to run the app
2. Use the Android Emulator that comes with the Android Studio installation (Slow & not advisable)
3. Use Genymotion Android Emulator
	* Go [here](https://www.genymotion.com/) and register for genymotion account if none. Free accounts have limitations which are not counter-productive
	* Download your OS Version of VirtualBox at [here](https://www.virtualbox.org/wiki/Downloads)
	* Install VirtualBox
	* Download Genymotion & Install it
	* Sign in to the genymotion app
	* Create a new Genymotion Virtual Device 
		* **Preferrable & Stable Choice** - API 22(Android 5.1.0), Screen size of around 800 X 1280, 1024 MB Memory --> eg. Google Nexus 7, Google Nexus 5

## Cont.. Android Studio Setup

1. Import the project into Android Studio by: **Import a gradle project** option
   _All the plugins required are explicitly stated therefore can work with any Android Studio version - Just enable it to download any packages not available offline_
1. Open Genymotion and Run the Virtual Device created previously.
1. Run the app on Android Studio and chose the Genymotion Emulator




## App Walkthrough (End-User)

1. **(Optional)** Open the app, open the app menu > Settings > Change url to your `server-url:port`. eg Point to Ona OpenSRP test server at `http://46.101.51.199:8080/opensrp`.

> **Note**
> Ona OpenSRP test server may not be up. You can follow [this Docker Setup](https://smartregister.atlassian.net/wiki/spaces/Documentation/pages/52690946/Docker+Setup) and [this Docker Compose Setup](https://smartregister.atlassian.net/wiki/spaces/Documentation/pages/52690976/Docker+Compose+Setup) guide for setting up your OpenSRP and OpenMRS Server.

![Settings Page](https://user-images.githubusercontent.com/31766075/30325652-37287d06-97ce-11e7-9039-5fd74dde2643.png)

2. Login using Sample Credentials(Provider Credentials) eg. 

	**Username:** biddemo
	**Password:** Linda123
 
	**Note:** _If the genymotion emulator **crashes on pressing the Login button**, use the **Enter Key** on your keyboard - (Reason for Crash) Genymotion emulator seems to have a bug_

	![Login Page](https://user-images.githubusercontent.com/31766075/30325639-36d671be-97ce-11e7-8999-c4b2ddf57005.png)

3. An activity with a list of patients (Child/Woman) is displayed with the following information. 
![List of patients Screenshot](https://user-images.githubusercontent.com/31766075/30325648-37044472-97ce-11e7-91fc-1f4236443ee3.png)

* Patient pictures
* Patient Name
* Age - In days, months or years
* Weight -If recorded, otherwise click on the button to record the weight currently or in the past
* Non-recurrent vaccine which is due in terms of duration eg. 6 weeks vaccine, 1 month vaccine --> They are color-coded based on _how-long-due_

	###### Service Status

	Color | Meaning
	----- | ---------
	Green | Administered/Given recently
	Red | Overdue
	Blue | Due soon (Due today OR Within 10 days after due-date )
	Light Blue | Upcoming
	White | Upcoming but not due anytime soon

* **Top Bar** _(Blue in Color)_ with:
	* ZEIR title - Zambia Electronic Immunization Registry
	* Current Health Facility OR Health Facility Zone
	* Add Patient Button **(Plus icon)**
	* Notification Icon showing total patients number who are **DUE** or **OVERDUE**
	* Search Icon - [Advanced search](#advanced-search) or [Global Search](#global-search)


* **Second Bar** _(Dark Grey in Color)_ has the following:

	* Search bar - Enter patient name OR Patient ID to search
	* Search patient by **[Scanning QR Code](#scanning-qr-code)**

### 4.1 Click on a patient

This opens a page which contains the immunisation history in a vaccine card.

##### Other details
+ Patient name
+ Patient Weight _(Two Options Available: Record past OR Current weight)_
+ Patient Weight Graph
+ Patient's Date of Birth - Written as ` DOB: 30/04/2016 `
+ Patient's Sibling's - Their pictures or Name _Abbreviations_
+ Patient's Recurring Immunisation and Other Health Services eg. Vitamin A, Deworming
+ Other Periodical immunization services & Their [status](#service-status) in the **Vaccine Card** (Service given, service due, service overdue, service delivered in the past).
	> * a **Ticked box** which shows that is was **administered**
	> * a **White box only** shows that is has not not been administered due to a past service - _That is, it has not been triggered or not-_
+ Record multiple immunisation services provided by clicking on the **Record all** button at the respective service provision duration eg. 6 weeks, 14 weeks, 9 months, 


As shown below:

![Patient Details](https://user-images.githubusercontent.com/31766075/30325644-36e0fae4-97ce-11e7-8045-f2bc5236d42c.png)


### 4.2 Click on the Health Facility Name

A dropdown menu appears with a list of the Health Facility Zones + Health Facility. The health facility name is the first, the rest are branches/zones of the health facility.

![Health Facility Dropdown Menu](https://user-images.githubusercontent.com/31766075/30325653-37290410-97ce-11e7-85b2-16346528a11a.png)

When chosen:
> - A Health Facility - A list of patients in that health facility from the various health zones will be shown. Arranged in descending order with the latest patient to be served at the to.
> - A Health Facility Zone - A list of patients in the specific Health Facility zone(Branch of the Health Facility) is shown. Arranged in descending order with the latest patient to be served at the to.

### 4.3 Click on the Plus Icon

A form to add a new patient to the current chosen Health Facility OR Health Facility Zone is openned.

As show below:
![New Patient Form](https://user-images.githubusercontent.com/31766075/30325642-36daad92-97ce-11e7-8094-3c9280613a1b.png)

The following details can be added:
- Photo of the Patient(Child)
- Child's home health facility (Current Health Facility/Zone selection by the provider)
- Child's ZEIR ID
- Child's Registeration Card Number
- Child's Birth Certificate Number
- Child's First Name
- Child's Last Name
- Child's Sex
- Child's Date of Birth
- Date that the child was first seen by the provider
- Child's Birth weight
- Child's Mother/Guardian:
   - First name
   - Last name
   - Date of Birth
   - NRC Number
- Child's Father/Guardian:
   - Full name
   - Date of Birth
   - NRC Number
- Child's Place of Birth
- Child's Residential Area
- Home Address
- Landmark
- CHW name
- CHW Phone Number
- HIV Exposure:

   Abbreviation Option | Meaning
   ------------------- | -------
   CE | Child Exposed
   MSU | Mother's State Unknown (The mother's HIV status before birth of the child was not checked)
   CNE | Child Not Exposed


### 4.4 Click on the search icon

This opens another activity where the provider can perform an [Advanced Search](#advanced-search) or [Global Search](#global-search) of a patient.

#### Advanced Search

This patient search enables one to search using other conditions or characteristics.

   * Using other search conditions such as 
      * Patient/Child Active status
      * First name/Last name
      * Mother's/guardian's name(s)
      * Date of birth within a certain range

![Advanced Search Page Screenshot](https://user-images.githubusercontent.com/31766075/30329131-18fc2498-97da-11e7-8683-12cf958e6be1.png)

#### Global Search

The global search enables the provider to search for a patient outside and inside the current health facility zone using the [Advanced Search](#advanced-search) above


### 4.5 Use the search bar

The search bar enables the provider to search for a patient by name or ZEIR ID.


### 4.6 Click on the **Scan QR Code** Button

Another activity will open and enable the phone camera for which one can scan the patient ID's QR Code as shown below. After the scan is complete, a patient(s) who matches the ZEIR ID scanned will be displayed.

![QR Code Scan Screenshot](https://user-images.githubusercontent.com/31766075/30329129-1822b5fa-97da-11e7-8a48-fcea85c6a639.png)

In case a match is **not found**

![QR Code Scan Match Not Found Screenshot](https://user-images.githubusercontent.com/31766075/30329130-18255422-97da-11e7-9af5-b3938a4ab7b2.png)


### 5. Slide Menu

There is a slide menu on the left in the Main Menu. 
This slide menu displays:
   * The current provider's name & their initials eg `Moh Zeir Demo` - `MZ`
   * **Plus Button** to add a patient
   * **Sync Button** - Manually sync records
   * **Submenu** with:
      * **ZEIR** - Current Page
      * **HIA 2 Reports** - View, Send and Edit HIA2 Reports
      * **Stock Control** - Stock Control page which enables the provider to:
         * Update stock levels with `newly issued vials`
         * Confirm stock levels
         * View stock levels for all drugs, vaccines and/or supplements - `vials` & `doses` available
         * Perform stock planning
         * Adjust stock levels to the available number - **For special cases & reason has to be provided**

### 6.1 Stock Control

**Coming soon...**



# Project Dependencies (Developer)

This project depends on the following OpenSRP modules/libraries:

1. OpenSRP Client native form 
2. OpenSRP Client Core
3. OpenSRP Client Immunization
4. OpenSRP Client Growth Monitoring



#### Folder Structure

```
opensrp-client-path
|__ opensrp-path
   |_ build
   |_ src
      |_ androidTest
      |_ main
         |_ assets 
             |_ fonts
             |_ json.form
         |_ java
             |_org.smartregister.path (main package)
             |_utils
   |_ test
```


Tests
======

The project mainly uses ```Junit``` & ```Mockito``` for tests which are all placed in the _test_ folder



