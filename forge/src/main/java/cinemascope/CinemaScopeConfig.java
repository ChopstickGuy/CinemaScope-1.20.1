package cinemascope;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Stored in config/cinema_scope.json. */
public final class CinemaScopeConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("CinemaScope");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FMLPaths.CONFIGDIR.get().resolve("cinema_scope.json");

    private static CinemaScopeConfig instance = new CinemaScopeConfig();

    /** Visible picture aspect ratio once the bars are fully in (2.39 = classic cinemascope). */
    public double targetAspectRatio = 2.39D;
    /**
     * Minimum height of each bar as a fraction of the screen height, so the effect stays visible on
     * screens that are already as wide as the target ratio (e.g. 21:9 ultrawide).
     */
    public double minBarFraction = 0.06D;
    /** How far to zoom in: 0.15 = FOV reduced by 15 %. */
    public double zoomAmount = 0.15D;
    /** Duration of the slide-in/slide-out animation in seconds. */
    public double animationSeconds = 1.2D;
    public boolean hideHud = true;
    public boolean hideHand = true;
    /** Enables Minecraft's smooth ("cinematic") camera while active. */
    public boolean smoothCamera = true;

    public static CinemaScopeConfig get() {
        return instance;
    }

    public static void load() {
        if (Files.exists(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH)) {
                CinemaScopeConfig loaded = GSON.fromJson(reader, CinemaScopeConfig.class);
                if (loaded != null) {
                    instance = loaded;
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to read {}, using defaults", PATH, e);
            }
        }
        instance.sanitize();
        save();
    }

    private static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(instance, writer);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save {}", PATH, e);
        }
    }

    private void sanitize() {
        targetAspectRatio = clamp(targetAspectRatio, 1.0D, 4.0D);
        minBarFraction = clamp(minBarFraction, 0.0D, 0.3D);
        zoomAmount = clamp(zoomAmount, 0.0D, 0.8D);
        animationSeconds = clamp(animationSeconds, 0.05D, 10.0D);
    }

    private static double clamp(double v, double min, double max) {
        return Double.isNaN(v) ? min : Math.max(min, Math.min(max, v));
    }
}
