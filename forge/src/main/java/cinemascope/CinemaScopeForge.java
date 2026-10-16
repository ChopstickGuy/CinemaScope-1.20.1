package cinemascope;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(CinemaScopeForge.MOD_ID)
public final class CinemaScopeForge {

    public static final String MOD_ID = "cinema_scope";

    public CinemaScopeForge() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            CinemaScopeConfig.load();
            FMLJavaModLoadingContext.get().getModEventBus().addListener(CinemaScopeClient::registerKeys);
            MinecraftForge.EVENT_BUS.register(CinemaScopeClient.class);
        }
    }
}
