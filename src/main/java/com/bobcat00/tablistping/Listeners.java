// TabListPing - Displays ping time in CraftBukkit/Spigot player list
// Copyright 2019 Bobcat00
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

package com.bobcat00.tablistping;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import com.bobcat00.tablistping.TabListPing;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Listeners implements Listener
{
    @SuppressWarnings("unused")
    private TabListPing plugin;
    private ProtocolManager protocolManager;
    
    private Map<UUID, Long> keepAliveTime = Collections.synchronizedMap(new HashMap<UUID, Long>());
    
    // Constructor
    
    public Listeners(TabListPing plugin)
    {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        // Hook in to ProtocolLib
        protocolManager = ProtocolLibrary.getProtocolManager();
        
        // Keep Alive from server to client
        
        protocolManager.addPacketListener(new PacketAdapter(plugin,
                                                            ListenerPriority.NORMAL,
                                                            PacketType.Play.Server.KEEP_ALIVE)
        {
            @Override
            public void onPacketSending(PacketEvent event)
            {
                if (event.getPacketType() == PacketType.Play.Server.KEEP_ALIVE)
                {
                    // Save time in hashmap
                    Player player = event.getPlayer();
                    UUID uuid = player.getUniqueId();
                    Long currentTime = System.currentTimeMillis();
                    keepAliveTime.put(uuid, currentTime); // possibly blocking
                }
            }
        });
        
        // Keep Alive response from client to server

        protocolManager.addPacketListener(new PacketAdapter(plugin,
                                                            ListenerPriority.NORMAL,
                                                            PacketType.Play.Client.KEEP_ALIVE)
        {
            @Override
            public void onPacketReceiving(PacketEvent event)
            {
                if (event.getPacketType() == PacketType.Play.Client.KEEP_ALIVE)
                {
                    // Get time from hashmap and calculate ping time in msec
                    Long currentTime = System.currentTimeMillis();
                    Player player = event.getPlayer();
                    UUID uuid = player.getUniqueId();
                    
                    Long pingTime = 0L;
                    Long startTime = keepAliveTime.get(uuid); // possibly blocking
                    if (startTime != null)
                    {
                        pingTime = currentTime - startTime;
                    }
                    
                    // Put ping time in tab list
                    String format = plugin.getConfig().getString("format");
                    player.setPlayerListName(ChatColor.translateAlternateColorCodes('&',
                            format.replace("%name%",        player.getName()).
                                   replace("%displayname%", player.getDisplayName()).
                                   replace("%ping%",        pingTime.toString())));
                }
            }
        });

    }
    
    // Player quit
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        // Remove player's entry from hashmap
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        keepAliveTime.remove(uuid); // possibly blocking
    }
    
}    
