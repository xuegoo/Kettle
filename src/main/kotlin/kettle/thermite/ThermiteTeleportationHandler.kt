package kettle.thermite

import org.bukkit.World.Environment

import net.minecraftforge.fml.common.FMLCommonHandler
import net.minecraftforge.fml.common.network.FMLEmbeddedChannel
import net.minecraftforge.fml.common.network.FMLOutboundHandler
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.network.play.server.S07PacketRespawn
import net.minecraft.network.play.server.S1DPacketEntityEffect
import net.minecraft.network.play.server.S1FPacketSetExperience
import net.minecraft.potion.PotionEffect
import net.minecraft.server.management.ServerConfigurationManager
import net.minecraft.util.MathHelper
import net.minecraft.util.math.MathHelper
import net.minecraft.world.WorldProvider
import net.minecraft.world.WorldServer
import net.minecraftforge.common.DimensionManager
import net.minecraftforge.common.network.ForgeMessage
import net.minecraftforge.common.network.ForgeNetworkHandler

object ThermiteTeleportationHandler {
    fun transferEntityToDimension(ent: Entity, dim: Int, manager: ServerConfigurationManager, environ: Environment) {

        if (ent is EntityPlayerMP) {
            transferPlayerToDimension(ent, dim, manager, environ)
            return
        }
        val worldserver = manager.getServerInstance().worldServerForDimension(ent.dimension)
        ent.dimension = dim
        val worldserver1 = manager.getServerInstance().worldServerForDimension(ent.dimension)
        worldserver.removePlayerEntityDangerously(ent)
        if (ent.riddenByEntity != null) {
            ent.riddenByEntity.mountEntity(null)
        }
        if (ent.ridingEntity != null) {
            ent.mountEntity(null)
        }
        ent.isDead = false
        transferEntityToWorld(ent, worldserver, worldserver1)
    }

    fun transferEntityToWorld(ent: Entity, oldWorld: WorldServer, newWorld: WorldServer) {

        val pOld = oldWorld.provider
        val pNew = newWorld.provider
        val moveFactor = pOld.movementFactor / pNew.movementFactor
        var x = ent.posX * moveFactor
        var z = ent.posZ * moveFactor
        x = MathHelper.clamp_double(x, -29999872, 29999872)
        z = MathHelper.clamp_double(z, -29999872, 29999872)

        if (ent.isEntityAlive) {
            ent.setLocationAndAngles(x, ent.posY, z, ent.rotationYaw, ent.rotationPitch)
            newWorld.spawnEntityInWorld(ent)
            newWorld.updateEntityWithOptionalForce(ent, false)
        }

        ent.setWorld(newWorld)
    }

    fun transferPlayerToDimension(player: EntityPlayerMP, dim: Int, manager: ServerConfigurationManager, environ: Environment) {

        val oldDim = player.dimension
        val worldserver = manager.getServerInstance().worldServerForDimension(player.dimension)
        player.dimension = dim
        val worldserver1 = manager.getServerInstance().worldServerForDimension(player.dimension)
        // Cauldron dont crash the client, let 'em know there's a new dimension in town
        if (DimensionManager.isBukkitDimension(dim)) {
            val serverChannel = ForgeNetworkHandler.getServerChannel()
            serverChannel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.PLAYER)
            serverChannel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(player)
            serverChannel.writeOutbound(ForgeMessage.DimensionRegisterMessage(dim, environ.id))
        }
        // Cauldron end
        player.playerNetServerHandler.sendPacket(S07PacketRespawn(dim, worldserver1.difficultySetting, worldserver1.getWorldInfo()
                .getTerrainType(), player.theItemInWorldManager.getGameType()))
        player.playerNetServerHandler.sendPacket(S1FPacketSetExperience(player.experience, player.experienceTotal, player.experienceLevel))

        worldserver.removePlayerEntityDangerously(player)
        if (player.riddenByEntity != null) {
            player.riddenByEntity.mountEntity(null)
        }
        if (player.ridingEntity != null) {
            player.mountEntity(null)
        }
        player.isDead = false
        transferEntityToWorld(player, worldserver, worldserver1)
        manager.func_72375_a(player, worldserver)
        player.playerNetServerHandler.setPlayerLocation(player.posX, player.posY, player.posZ, player.rotationYaw, player.rotationPitch)
        player.theItemInWorldManager.setWorld(worldserver1)
        manager.updateTimeAndWeatherForPlayer(player, worldserver1)
        manager.syncPlayerInventory(player)
        val iterator = player.activePotionEffects.iterator()

        while (iterator.hasNext()) {
            val potioneffect = iterator.next()
            player.playerNetServerHandler.sendPacket(S1DPacketEntityEffect(player.entityId, potioneffect))
        }
        FMLCommonHandler.instance().firePlayerChangedDimensionEvent(player, oldDim, dim)
    }
}
