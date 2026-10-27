package io.izzel.arclight.common.mixin.core.server.rcon.thread;

import com.destroystokyo.paper.event.server.GS4QueryEvent;
import io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.server.ServerInterface;
import net.minecraft.server.rcon.thread.QueryThreadGs4;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.plugin.Plugin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.List;

@Mixin(QueryThreadGs4.class)
public abstract class QueryThreadGs4EventMixin {

    // @formatter:off
    @Shadow @Final private ServerInterface serverInterface;
    @Shadow @Final private String worldName;
    @Shadow @Final private int maxPlayers;
    @Shadow @Final private int serverPort;
    @Shadow private String hostIp;
    // @formatter:on

    @Decorate(method = "processPacket(Ljava/net/DatagramPacket;)Z",
        at = @At(value = "FIELD", target = "Lnet/minecraft/server/rcon/thread/QueryThreadGs4;serverName:Ljava/lang/String;"),
        require = 1, allow = 1)
    private String arclight$basicQueryEvent(QueryThreadGs4 queryThread,
                                          @Local(ordinal = 0) DatagramPacket packet,
                                          @Local(allocate = "arclightQueryResponse") GS4QueryEvent.QueryResponse response) throws Throwable {
        String motd = (String) DecorationOps.callsite().invoke(queryThread);
        response = this.arclight$callQueryEvent(GS4QueryEvent.QueryType.BASIC, packet, motd);
        DecorationOps.blackhole().invoke(response);
        return response.getMotd();
    }

    @Decorate(method = "buildRuleResponse(Ljava/net/DatagramPacket;)[B",
        at = @At(value = "FIELD", target = "Lnet/minecraft/server/rcon/thread/QueryThreadGs4;serverName:Ljava/lang/String;"),
        require = 1, allow = 1)
    private String arclight$fullQueryEvent(QueryThreadGs4 queryThread,
                                         @Local(ordinal = 0) DatagramPacket packet,
                                         @Local(allocate = "arclightQueryResponse") GS4QueryEvent.QueryResponse response) throws Throwable {
        String motd = (String) DecorationOps.callsite().invoke(queryThread);
        // The native cache branch never reaches this field: retain Paper's five-second full-response cache.
        response = this.arclight$callQueryEvent(GS4QueryEvent.QueryType.FULL, packet, motd);
        DecorationOps.blackhole().invoke(response);
        return response.getMotd();
    }

    @Decorate(method = {"processPacket(Ljava/net/DatagramPacket;)Z", "buildRuleResponse(Ljava/net/DatagramPacket;)[B"},
        at = @At(value = "FIELD", target = "Lnet/minecraft/server/rcon/thread/QueryThreadGs4;worldName:Ljava/lang/String;"),
        require = 2, expect = 2, allow = 2)
    private String arclight$queryMap(QueryThreadGs4 queryThread,
                                    @Local(allocate = "arclightQueryResponse") GS4QueryEvent.QueryResponse response) throws Throwable {
        String original = (String) DecorationOps.callsite().invoke(queryThread);
        return response == null ? original : response.getMap();
    }

    @Decorate(method = {"processPacket(Ljava/net/DatagramPacket;)Z", "buildRuleResponse(Ljava/net/DatagramPacket;)[B"},
        at = @At(value = "FIELD", target = "Lnet/minecraft/server/rcon/thread/QueryThreadGs4;maxPlayers:I"),
        require = 2, expect = 2, allow = 2)
    private int arclight$queryMaxPlayers(QueryThreadGs4 queryThread,
                                         @Local(allocate = "arclightQueryResponse") GS4QueryEvent.QueryResponse response) throws Throwable {
        int original = (int) DecorationOps.callsite().invoke(queryThread);
        return response == null ? original : response.getMaxPlayers();
    }

    @Decorate(method = {"processPacket(Ljava/net/DatagramPacket;)Z", "buildRuleResponse(Ljava/net/DatagramPacket;)[B"},
        at = @At(value = "FIELD", target = "Lnet/minecraft/server/rcon/thread/QueryThreadGs4;serverPort:I"),
        require = 2, expect = 2, allow = 2)
    private int arclight$queryPort(QueryThreadGs4 queryThread,
                                   @Local(allocate = "arclightQueryResponse") GS4QueryEvent.QueryResponse response) throws Throwable {
        int original = (int) DecorationOps.callsite().invoke(queryThread);
        // Keep the native BASIC little-endian short and FULL decimal-string encoders.
        return response == null ? original : response.getPort();
    }

    @Decorate(method = {"processPacket(Ljava/net/DatagramPacket;)Z", "buildRuleResponse(Ljava/net/DatagramPacket;)[B"},
        at = @At(value = "FIELD", target = "Lnet/minecraft/server/rcon/thread/QueryThreadGs4;hostIp:Ljava/lang/String;"),
        require = 2, expect = 2, allow = 2)
    private String arclight$queryHostname(QueryThreadGs4 queryThread,
                                         @Local(allocate = "arclightQueryResponse") GS4QueryEvent.QueryResponse response) throws Throwable {
        String original = (String) DecorationOps.callsite().invoke(queryThread);
        return response == null ? original : response.getHostname();
    }

    @Decorate(method = {"processPacket(Ljava/net/DatagramPacket;)Z", "buildRuleResponse(Ljava/net/DatagramPacket;)[B"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerInterface;getPlayerCount()I"),
        require = 2, expect = 2, allow = 2)
    private int arclight$queryCurrentPlayers(ServerInterface server,
                                             @Local(allocate = "arclightQueryResponse") GS4QueryEvent.QueryResponse response) throws Throwable {
        int original = (int) DecorationOps.callsite().invoke(server);
        return response == null ? original : response.getCurrentPlayers();
    }

    @Decorate(method = "buildRuleResponse(Ljava/net/DatagramPacket;)[B",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerInterface;getServerVersion()Ljava/lang/String;"),
        require = 1, allow = 1)
    private String arclight$queryGameVersion(ServerInterface server,
                                            @Local(allocate = "arclightQueryResponse") GS4QueryEvent.QueryResponse response) throws Throwable {
        String original = (String) DecorationOps.callsite().invoke(server);
        return response == null ? original : response.getGameVersion();
    }

    @Decorate(method = "buildRuleResponse(Ljava/net/DatagramPacket;)[B",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerInterface;getPluginNames()Ljava/lang/String;"),
        require = 1, allow = 1)
    private String arclight$queryPlugins(ServerInterface server,
                                        @Local(allocate = "arclightQueryResponse") GS4QueryEvent.QueryResponse response) throws Throwable {
        String original = (String) DecorationOps.callsite().invoke(server);
        if (response == null) {
            return original;
        }
        StringBuilder plugins = new StringBuilder(response.getServerVersion());
        if (!response.getPlugins().isEmpty()) {
            plugins.append(": ");
            var iterator = response.getPlugins().iterator();
            while (iterator.hasNext()) {
                GS4QueryEvent.QueryResponse.PluginInformation plugin = iterator.next();
                plugins.append(plugin.getName());
                if (plugin.getVersion() != null) {
                    plugins.append(' ').append(plugin.getVersion().replace(";", ","));
                }
                if (iterator.hasNext()) {
                    plugins.append("; ");
                }
            }
        }
        return plugins.toString();
    }

    @Decorate(method = "buildRuleResponse(Ljava/net/DatagramPacket;)[B",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerInterface;getPlayerNames()[Ljava/lang/String;"),
        require = 1, allow = 1)
    private String[] arclight$queryPlayerNames(ServerInterface server,
                                              @Local(allocate = "arclightQueryResponse") GS4QueryEvent.QueryResponse response) throws Throwable {
        String[] original = (String[]) DecorationOps.callsite().invoke(server);
        return response == null ? original : response.getPlayers().toArray(String[]::new);
    }

    @Unique
    private GS4QueryEvent.QueryResponse arclight$callQueryEvent(GS4QueryEvent.QueryType type, DatagramPacket packet, String motd) {
        CraftServer server = ((MinecraftServerBridge) this.serverInterface).bridge$getServer();
        GS4QueryEvent.QueryResponse.Builder builder = GS4QueryEvent.QueryResponse.builder()
            .motd(motd)
            .map(this.worldName)
            .currentPlayers(this.serverInterface.getPlayerCount())
            .maxPlayers(this.maxPlayers)
            .port(this.serverPort)
            .hostname(this.hostIp)
            .gameVersion(this.serverInterface.getServerVersion())
            .serverVersion(server.getName() + " on " + server.getBukkitVersion());
        if (type == GS4QueryEvent.QueryType.FULL) {
            List<GS4QueryEvent.QueryResponse.PluginInformation> plugins = new ArrayList<>();
            if (server.getQueryPlugins()) {
                for (Plugin plugin : server.getPluginManager().getPlugins()) {
                    plugins.add(GS4QueryEvent.QueryResponse.PluginInformation.of(plugin.getName(), plugin.getDescription().getVersion()));
                }
            }
            builder.plugins(plugins).players(this.serverInterface.getPlayerNames());
        }
        GS4QueryEvent event = new GS4QueryEvent(type, packet.getAddress(), builder.build());
        server.getPluginManager().callEvent(event);
        return event.getResponse();
    }
}
