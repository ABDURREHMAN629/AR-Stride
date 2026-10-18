package com.stride.fit

/**
 * The name the app uses for itself on screen. Keep it in step with `app_name` in
 * res/values/strings.xml, which is the launcher label and the name system Settings
 * shows - messages that send the user into Settings quote this constant.
 *
 * The package id (`com.stride.fit`) deliberately does not follow the display name:
 * changing it would make Android treat this as a different app, orphaning every step,
 * run and workout already recorded.
 */
object Brand {
    const val NAME = "AR-Stride"
}
