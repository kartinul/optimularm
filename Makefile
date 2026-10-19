MAC_BUILD := build/mac
MAC_APP := $(MAC_BUILD)/Build/Products/Release/optimularm.app
XCODEBUILD := xcodebuild -project macos/optimularm.xcodeproj -scheme optimularm -derivedDataPath $(MAC_BUILD) -allowProvisioningUpdates

.PHONY: install mac mac-install android android-install test rules logs

install: mac-install android-install

mac:
	$(XCODEBUILD) -configuration Release build

# quits the running copy, replaces /Applications/optimularm.app and starts it (it enables launch at login on first run)
mac-install: mac
	-pkill -x optimularm
	rm -rf /Applications/optimularm.app
	cp -R $(MAC_APP) /Applications/
	open /Applications/optimularm.app

android:
	cd android && ./gradlew assembleDebug

android-install:
	cd android && ./gradlew installDebug
	adb shell am start -n io.github.kartinul/.MainActivity

test:
	cd android && ./gradlew test

rules:
	firebase deploy --only firestore:rules,auth

logs:
	adb logcat -s optimularm
