package com.mirzadev.onecenter.ui.design

enum class BackgroundType(val label: String) {
    Solid("Solid"),
    Liquid("Liquid");

    companion object {
        val Default = Liquid
    }
}
