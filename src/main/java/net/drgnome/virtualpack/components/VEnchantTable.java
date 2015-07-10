// Bukkit Plugin "VirtualPack" by Siguza
// The license under which this software is released can be accessed at:
// http://creativecommons.org/licenses/by-nc-sa/3.0/

package net.drgnome.virtualpack.components;

import java.util.*;
import java.lang.reflect.*;
import net.minecraft.server.v#MC_VERSION#.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.enchantment.*;
import org.bukkit.craftbukkit.v#MC_VERSION#.CraftServer;
import org.bukkit.craftbukkit.v#MC_VERSION#.inventory.CraftItemStack;
import net.drgnome.virtualpack.util.*;

public class VEnchantTable extends ContainerEnchantTable
{
    private final EntityPlayer mcPlayer;
    private final Player player;
    private final World world;
    private final Random rand = new Random();
    private final int bookshelves;

    public VEnchantTable(EntityPlayer player, int bookshelves)
    {
        super(player.inventory, player.world, new BlockPosition(0, 0, 0));
        this.checkReachable = false;
        this.bookshelves = bookshelves;
        this.mcPlayer = player;
        this.player = (Player)player.getBukkitEntity();
        this.world = player.world;
    }

    public final ItemStack clickItem(int slot, int mouse, int shift, EntityHuman human)
    {
        ItemStack stack = super.clickItem(slot, mouse, shift, human);
        mcPlayer.updateInventory(mcPlayer.activeContainer);
        return stack;
    }

    public void #FIELD_CONTAINER_6#(IInventory iinventory)
    {
        if(iinventory == this.enchantSlots)
        {
            ItemStack itemstack = iinventory.getItem(0);
            int i;
            if(itemstack != null)
            {
                if(!this.world.#FIELD_WORLD_1#)
                {
                    int j;
                    rand.setSeed((long)this.#FIELD_CONTAINERENCHANTTABLE_3#);
                    for(j = 0; j < 3; ++j)
                    {
                        this.costs[j] = EnchantmentManager.#FIELD_ENCHANTMENTMANAGER_1#(rand, j, bookshelves, itemstack);
                        this.h[j] = -1;
                        if(this.costs[j] < j + 1)
                        {
                            this.costs[j] = 0;
                        }
                    }
                    CraftItemStack item = CraftItemStack.asCraftMirror(itemstack);
                    PrepareItemEnchantEvent event = new PrepareItemEnchantEvent(player, this.getBukkitView(), this.world.getWorld().getBlockAt(0, 0, 0), item, this.costs, bookshelves);
                    event.setCancelled(!itemstack.v());
                    this.world.getServer().getPluginManager().callEvent(event);
                    if(event.isCancelled() && !Config.bool("events.ignorecancelled"))
                    {
                        for(i = 0; i < 3; ++i)
                        {
                            this.costs[i] = 0;
                        }
                        return;
                    }
                    for(j = 0; j < 3; ++j)
                    {
                        if(this.costs[j] > 0)
                        {
                            List list = getWeightedRandomEnchantList(itemstack, j, this.costs[j]);
                            if(list != null && !list.isEmpty())
                            {
                                WeightedRandomEnchant weightedrandomenchant = (WeightedRandomEnchant) list.get(rand.nextInt(list.size()));
                                this.h[j] = weightedrandomenchant.enchantment.id | weightedrandomenchant.level << 8;
                            }
                        }
                    }
                    this.#FIELD_CONTAINERENCHANTTABLE_4#();
                }
            }
            else
            {
                for(i = 0; i < 3; ++i)
                {
                    this.costs[i] = 0;
                    this.h[i] = -1;
                }
            }
        }
    }

    public boolean #FIELD_CONTAINERENCHANTTABLE_2#(EntityHuman entityhuman, int i)
    {
        ItemStack itemstack = this.enchantSlots.getItem(0);
        ItemStack itemstack1 = this.enchantSlots.getItem(1);
        int j = i + 1;
        if((itemstack1 == null || itemstack1.count < j) && !playerFree(entityhuman))
        {
            return false;
        }
        else if(this.costs[i] > 0 && itemstack != null && (entityhuman.expLevel >= j && entityhuman.expLevel >= this.costs[i] || playerFree(entityhuman)))
        {
            if(!this.world.#FIELD_WORLD_1#)
            {
                List list = getWeightedRandomEnchantList(itemstack, i, this.costs[i]);
                if(list == null)
                {
                    list = new java.util.ArrayList<WeightedRandomEnchant>();
                }
                boolean flag = itemstack.getItem() == Items.BOOK;
                if(list != null)
                {
                    Map<org.bukkit.enchantments.Enchantment, Integer> enchants = new java.util.HashMap<org.bukkit.enchantments.Enchantment, Integer>();
                    for(Object obj : list)
                    {
                        WeightedRandomEnchant instance = (WeightedRandomEnchant)obj;
                        enchants.put(org.bukkit.enchantments.Enchantment.getById(instance.enchantment.id), instance.level);
                    }
                    CraftItemStack item = CraftItemStack.asCraftMirror(itemstack);
                    EnchantItemEvent event = new EnchantItemEvent((Player)entityhuman.getBukkitEntity(), this.getBukkitView(), this.world.getWorld().getBlockAt(0, 0, 0), item, this.costs[i], enchants, i);
                    this.world.getServer().getPluginManager().callEvent(event);
                    int level = event.getExpLevelCost();
                    if((event.isCancelled() && !Config.bool("events.ignorecancelled")) || (level > entityhuman.expLevel && !playerFree(entityhuman)) || event.getEnchantsToAdd().isEmpty())
                    {
                        return false;
                    }
                    if(flag)
                    {
                        itemstack.setItem(Items.ENCHANTED_BOOK);
                    }
                    for(Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : event.getEnchantsToAdd().entrySet())
                    {
                        try
                        {
                            if(flag)
                            {
                                int enchantId = entry.getKey().getId();
                                if(Enchantment.getById(enchantId) == null)
                                {
                                    continue;
                                }
                                WeightedRandomEnchant enchantment = new WeightedRandomEnchant(Enchantment.getById(enchantId), entry.getValue());
                                Items.ENCHANTED_BOOK.a(itemstack, enchantment);
                            }
                            else
                            {
                                item.addUnsafeEnchantment(entry.getKey(), entry.getValue());
                            }
                        }
                        catch(IllegalArgumentException e)
                        {
                            /* Just swallow invalid enchantments */
                        }
                    }
                    entityhuman.#FIELD_ENTITYHUMAN_1#(j);
                    if(!playerFree(entityhuman))
                    {
                        itemstack1.count -= j;
                        if(itemstack1.count <= 0)
                        {
                            this.enchantSlots.setItem(1, (ItemStack) null);
                        }
                    }
                    ---------- SINCE 1.8.3 START ----------
                    entityhuman.#FIELD_ENTITYHUMAN_2#(StatisticList.#FIELD_STATISTICSLIST_1#);
                    ---------- SINCE 1.8.3 END ----------
                    this.enchantSlots.update();
                    this.#FIELD_CONTAINERENCHANTTABLE_3# = entityhuman.#FIELD_ENTITYHUMAN_3#();
                    this.#FIELD_CONTAINER_6#(this.enchantSlots);
                }
            }
            return true;
        }
        else
        {
            return false;
        }
    }

    private List<WeightedRandomEnchant> getWeightedRandomEnchantList(ItemStack itemstack, int i, int j)
    {
        rand.setSeed((long)(this.#FIELD_CONTAINERENCHANTTABLE_3# + i));
        List list = EnchantmentManager.b(rand, itemstack, j);
        if(itemstack.getItem() == Items.BOOK && list != null && list.size() > 1)
        {
            list.remove(rand.nextInt(list.size()));
        }
        return list;
    }

    public static boolean playerFree(EntityHuman entityhuman)
    {
        return entityhuman.abilities.canInstantlyBuild || Perm.has(entityhuman.world.getWorld().getName(), (Player)entityhuman.getBukkitEntity(), "vpack.use.enchanttable.free");
    }
}
