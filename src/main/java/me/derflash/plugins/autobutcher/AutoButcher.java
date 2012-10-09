package me.derflash.plugins.autobutcher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class AutoButcher extends JavaPlugin implements Listener {
	static AutoButcher p;
	int perSweep;
	int maxAmount;
	int radius;
	int sweep;
	boolean taskAlreadyRunning;
	
	FileConfiguration config;
	
    public void onDisable() {
    	stopTask();
    	saveConfig();
    }

    public void onEnable() {
    	AutoButcher.p = this;
    	
        getServer().getPluginManager().registerEvents(this, this);
        loadConfig();
		if (config.getBoolean("taskRunning", false)) startTask();

    }
    
    public void loadConfig() {
        config = getConfig();
        
        perSweep = config.getInt("perSweep", 10);
        maxAmount = config.getInt("maxAmount", 10);
		radius = config.getInt("radius", 1);
        sweep = config.getInt("sweep", 6000);
    }
    
    public boolean onCommand(CommandSender sender, Command cmd, String label, final String[] args) {
    	final Player player = (Player) sender;
		if (!player.hasPermission("autobutcher.admin")) return true;
		if (args.length == 0) {
			player.sendMessage(ChatColor.AQUA + "[AutoButcher] " + ChatColor.DARK_AQUA + "Hi!");
			return true;
		}
    	
		if(args[0].equalsIgnoreCase("persweep")) {
			if (args.length > 1) {
				perSweep = Integer.parseInt(args[1]);
				config.set("perSweep", perSweep);
				player.sendMessage(ChatColor.AQUA + "[AutoButcher] " + ChatColor.DARK_AQUA + "PerSweep set to: " + ChatColor.AQUA + perSweep);
				saveConfig();				
			} else {
				player.sendMessage(ChatColor.AQUA + "[AutoButcher] " + ChatColor.DARK_AQUA + "Current perSweep: " + ChatColor.AQUA + perSweep);

			}

		} else if(args[0].equalsIgnoreCase("maxamount")) {
			if (args.length > 1) {
				maxAmount = Integer.parseInt(args[1]);
				config.set("maxAmount", maxAmount);
				player.sendMessage(ChatColor.AQUA + "[AutoButcher] " + ChatColor.DARK_AQUA + "MaxAmount set to: " + ChatColor.AQUA + maxAmount);
				saveConfig();
				
			} else {
				player.sendMessage(ChatColor.AQUA + "[AutoButcher] " + ChatColor.DARK_AQUA + "Current maxAmount: " + ChatColor.AQUA + maxAmount);
	
			}

		} else if(args[0].equalsIgnoreCase("radius")) {
			if (args.length > 1) {
				radius = Integer.parseInt(args[1]);
				config.set("radius", radius);
				player.sendMessage(ChatColor.AQUA + "[AutoButcher] " + ChatColor.DARK_AQUA + "Radius set to: " + ChatColor.AQUA + radius);
				saveConfig();
				
			} else {
				player.sendMessage(ChatColor.AQUA + "[AutoButcher] " + ChatColor.DARK_AQUA + "Current radius: " + ChatColor.AQUA + radius);
				
			}
			
		} else if(args[0].equalsIgnoreCase("sweep")) {
			if (args.length > 1) {
				sweep = Integer.parseInt(args[1]);
				config.set("sweep", sweep);
				player.sendMessage(ChatColor.AQUA + "[AutoButcher] " + ChatColor.DARK_AQUA + "Sweep rate set to: " + ChatColor.AQUA + sweep + " (restarttask to apply)");
				saveConfig();
				
			} else {
				player.sendMessage(ChatColor.AQUA + "[AutoButcher] " + ChatColor.DARK_AQUA + "Current sweep rate: " + ChatColor.AQUA + sweep);
				
			}

		} else if(args[0].equalsIgnoreCase("run")) {
	    	if (taskAlreadyRunning) {
				player.sendMessage(ChatColor.AQUA + "[AutoButcher] " + ChatColor.DARK_AQUA + "Another butcher task is already active");
				
	    	} else {
				player.sendMessage(ChatColor.AQUA + "[AutoButcher] " + ChatColor.DARK_AQUA + "Butcher task scheduled");
				
		    	getServer().getScheduler().scheduleAsyncDelayedTask(this, new Runnable() {public void run() {
			    	aSyncTaskToRun(player, (args.length > 1 ? Integer.parseInt(args[1]) : 0) );
				}});
	    		
	    	}

		} else if(args[0].equalsIgnoreCase("restarttask")) {
			stopTask();
			startTask();
			config.set("taskRunning", true);
			player.sendMessage(ChatColor.AQUA + "[AutoButcher] " + ChatColor.DARK_AQUA + "Restarted task");

		} else if(args[0].equalsIgnoreCase("starttask")) {
			startTask();
			config.set("taskRunning", true);
			player.sendMessage(ChatColor.AQUA + "[AutoButcher] " + ChatColor.DARK_AQUA + "Started task");
			
		} else if(args[0].equalsIgnoreCase("stoptask")) {
			stopTask();
			config.set("taskRunning", false);
			player.sendMessage(ChatColor.AQUA + "[AutoButcher] " + ChatColor.DARK_AQUA + "Stopped task");

		} else if(args[0].equalsIgnoreCase("reload")) {
			player.sendMessage(ChatColor.AQUA + "[AutoButcher] " + ChatColor.DARK_AQUA + "Reloading...");
			stopTask();
			reloadConfig();
			loadConfig();
			if (config.getBoolean("taskRunning", false)) {
				player.sendMessage(ChatColor.AQUA + "[AutoButcher] " + ChatColor.DARK_AQUA + "Restarted task");
				startTask();
			}
		}
		
		return true;
    }
    
    private void stopTask() {
    	getServer().getScheduler().cancelTasks(this);
    }
    
    private void startTask() {
    	this.getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() {
        	   public void run() {
        		   aSyncTaskToRun(null, 0);
        	   }
        	}, 200L, sweep);
    }

    
    private boolean aSyncTaskToRun(Player player, int perSweepOverride) {
    	
    	if (taskAlreadyRunning) {
    		if (player != null) player.sendMessage(ChatColor.AQUA + "[AutoButcher] " + ChatColor.DARK_AQUA + "Another butcher task was already active");
    		return false;
    	}
    	
    	getLogger().info("Checking ...");
    	taskAlreadyRunning = true;

    	int counter = 0;
    	if (perSweepOverride == 0) perSweepOverride = perSweep;
    	ArrayList<Entity> _toRemove = new ArrayList<Entity>();
    	ArrayList<Entity> _checked = new ArrayList<Entity>();
    	
		List<World> worlds = null;
		try {
			worlds = getServer().getScheduler().callSyncMethod(AutoButcher.p, new Callable<List<World>>() { public List<World> call() throws Exception {
				return getServer().getWorlds();
			}}).get();
		} catch (Exception e){}
		if (worlds == null) return true;

    	for (final World world : worlds) {
    		
    		Chunk[] chunks = null;
			try {
				chunks = getServer().getScheduler().callSyncMethod(AutoButcher.p, new Callable<Chunk[]>() { public Chunk[] call() throws Exception {
					return world.getLoadedChunks();
				}}).get();
			} catch (Exception e){}
			if (chunks == null) continue;
			
    		for (final Chunk chunk : chunks) {
    			
        		Entity[] entities = null;
    			try {
    				entities = getServer().getScheduler().callSyncMethod(AutoButcher.p, new Callable<Entity[]>() { public Entity[] call() throws Exception {
    					return chunk.getEntities();
    				}}).get();
    			} catch (Exception e){}
    			if (entities == null) continue;
    			
    			for (final Entity entity : entities) {
    				if (!(entity instanceof Creature)) continue;
    				if (_toRemove.contains(entity)) continue;
    				
    				// mask as checked, so we do not delete this one
    				_checked.add(entity);

    				List<Entity> surround = null;
    				try {
    					surround = getServer().getScheduler().callSyncMethod(AutoButcher.p, new Callable<List<Entity>>() { public List<Entity> call() throws Exception {
    						return entity.getNearbyEntities(radius,radius,radius);
    					}}).get();
    				} catch (Exception e){}
    				if (surround == null) continue;

    				// find creatures of same type
    				ArrayList<Entity> sameTypes = new ArrayList<Entity>();
    				EntityType type = entity.getType();
    				for (Entity sEntity : surround) {
    					if (sEntity instanceof Creature) {
    						if (sEntity.getType().equals(type)) {
    		    				if (_checked == null || !_checked.contains(sEntity)) {
        							sameTypes.add(sEntity);
    		    				}
    						}
    					}
    				}

    				
    				if (sameTypes.size() > maxAmount) {
    					Location locE = entity.getLocation();
    					getLogger().info("Found a " + type.getName() + " at " + locE.getWorld().getName() + "["+locE.getBlockX()+","+locE.getBlockY()+","+locE.getBlockZ()+"] surrounded by " + surround.size() + " others...");
    					
        				while (sameTypes.size() > maxAmount) {
        					Entity removed = sameTypes.remove(0);
        					if (!_toRemove.contains(removed)) {
            					Location locR = removed.getLocation();
            					getLogger().info("Scheduling entity removal: " + removed.toString() + " at " + locR.getWorld().getName() + "["+locR.getBlockX()+","+locR.getBlockY()+","+locR.getBlockZ()+"]");
        						
        						_toRemove.add(removed);
            					counter++;
        					}

        					if (counter >= perSweepOverride) break;
        				}
    				}

    				if (counter >= perSweepOverride) break;
    			}
    			if (counter >= perSweepOverride) break;
    		}
    		if (counter >= perSweepOverride) break;
    	}

    	if (_toRemove.size() == 0) {
    		if (player != null) player.sendMessage(ChatColor.AQUA + "[AutoButcher] " + ChatColor.DARK_AQUA + "No butcher was needed");
			getLogger().info("No butcher was needed");

    	} else {
    		final ArrayList<Entity> toRemove = new ArrayList<Entity>(_toRemove);

    		if (player != null) player.sendMessage(ChatColor.AQUA + "[AutoButcher] " + ChatColor.DARK_AQUA + "Found " + toRemove.size() + " entities to remove");

    		try {
    			getServer().getScheduler().callSyncMethod(AutoButcher.p, new Callable<Object>() { public Object call() throws Exception {
					getLogger().info("Removing " + toRemove.size() + " scheduled entities");
    				for (Entity goRemove : toRemove) {
    					goRemove.remove();
    				}
					getLogger().info("Removed all scheduled entities");
    				return null;
    			}}).get();
    		} catch (InterruptedException e) {
    			e.printStackTrace();
    		} catch (ExecutionException e) {
    			e.printStackTrace();
    		}
    	}

    	taskAlreadyRunning = false;
    	
    	return true;
    }

}

