package kettle

import net.minecraftforge.fml.common.FMLCommonHandler
import org.spigotmc.RestartCommand
import java.io.File
import java.net.URLDecoder
import java.util.*

object Kettle {
    val sKettleThreadGroup = ThreadGroup("Kettle")

    private var sManifestParsed = false

    private var sCurrentVersion: String? = null

    val currentVersion: String?
        get() {
            parseManifest()
            return sCurrentVersion
        }

    private var sServerLocation: File? = null

    val serverLocation: File?
        get() {
            parseManifest()
            return sServerLocation
        }

    private var sServerHome: File? = null

    val serverHome: File?
        get() {
            if (sServerHome == null) {
                val home = System.getenv("KETTLE_HOME")
                if (home != null) {
                    sServerHome = File(home)
                } else {
                    parseManifest()
                    sServerHome = sServerLocation!!.parentFile
                }
            }
            return sServerHome
        }

    private var sGroup: String? = null

    val group: String?
        get() {
            parseManifest()
            return sGroup
        }

    private var sBranch: String? = null

    val branch: String?
        get() {
            parseManifest()
            return sBranch
        }

    private var sChannel: String? = null

    val channel: String?
        get() {
            parseManifest()
            return sChannel
        }

    private var sLegacy: Boolean = false
    private var sOfficial: Boolean = false

    val isLegacy: Boolean
        get() {
            parseManifest()
            return sLegacy
        }

    val isOfficial: Boolean
        get() {
            parseManifest()
            return sOfficial
        }

    var sNewServerLocation: File? = null
    var sNewServerVersion: String? = null
    var sUpdateInProgress: Boolean = false

    private var sForgeRevision = 0

    private fun parseManifest() {
        if (sManifestParsed)
            return
        sManifestParsed = true

        try {
            val resources = Kettle::class.java.classLoader
                    .getResources("META-INF/MANIFEST.MF")
            val manifest = Properties()
            while (resources.hasMoreElements()) {
                val url = resources.nextElement()
                manifest.load(url.openStream())
                val version = manifest.getProperty("Kettle-Version")
                if (version != null) {
                    val path = url.path
                    var jarFilePath = path.substring(path.indexOf(":") + 1,
                            path.indexOf("!"))
                    jarFilePath = URLDecoder.decode(jarFilePath, "UTF-8")
                    sServerLocation = File(jarFilePath)

                    sCurrentVersion = version
                    sGroup = manifest.getProperty("Kettle-Group")
                    sBranch = manifest.getProperty("Kettle-Branch")
                    sChannel = manifest.getProperty("Kettle-Channel")
                    sLegacy = java.lang.Boolean.parseBoolean(manifest.getProperty("Kettle-Legacy"))
                    sOfficial = java.lang.Boolean.parseBoolean(manifest.getProperty("Kettle-Official"))
                    break
                }
                manifest.clear()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun restart() {
        RestartCommand.restart(true)
    }

    fun lookupForgeRevision(): Int {
        if (sForgeRevision != 0) return sForgeRevision
        var revision = Integer.parseInt(System.getProperty("Kettle.forgeRevision", "0"))
        if (revision != 0) {
            sForgeRevision = revision
            return sForgeRevision
        }
        try {
            val p = Properties()
            p.load(Kettle::class.java
                    .getResourceAsStream("/fmlversion.properties"))
            revision = Integer.parseInt(p.getProperty(
                    "fmlbuild.build.number", "0").toString())
        } catch (e: Exception) {
        }

        if (revision == 0) {
            KLog.get().warning("Kettle: could not parse forge revision, critical error")
            FMLCommonHandler.instance().exitJava(1, false)
        }
        sForgeRevision = revision
        return sForgeRevision
    }
}
