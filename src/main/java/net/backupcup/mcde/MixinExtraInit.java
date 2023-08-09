package net.backupcup.mcde;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class MixinExtraInit implements PreLaunchEntrypoint {
    public void onPreLaunch() {
        MixinExtrasBootstrap.init();
    }
}
