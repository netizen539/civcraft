package com.avrgaming.civcraft.tutorial;

import gpl.AttributeUtil;

import java.util.LinkedList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.ConfigMaterial;
import com.avrgaming.civcraft.config.ConfigMaterialCategory;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.lorestorage.LoreGuiItemListener;
import com.avrgaming.civcraft.lorestorage.LoreMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;

public class CivTutorial {

	public static Inventory tutorialInventory = null;
	public static Inventory craftingHelpInventory = null;
	public static Inventory guiInventory = null;
	public static final int MAX_CHEST_SIZE = 6;
	
	public static void showTutorialInventory(Player player) {	
		if (tutorialInventory == null) {
			tutorialInventory = Bukkit.getServer().createInventory(player, 9*3, "CivCraft Tutorial");
		
	
			tutorialInventory.addItem(LoreGuiItem.build(CivColor.LightBlue+ChatColor.BOLD+"What is CivCraft?", ItemManager.getId(Material.WORKBENCH), 0, 
				ChatColor.RESET+"CivCraft is a game about building civilizations set in a large,",
				ChatColor.RESET+"persistent world filled with players.",
				ChatColor.RESET+"Players start out as nomads, gathering",
				ChatColor.RESET+"resources and making allies until they can build a camp.",
				ChatColor.RESET+"Gather more resources and allies and found a civilization!",
				ChatColor.RESET+CivColor.LightGreen+"Research technology! Build structures! Conquer the world!"
				));
		
			tutorialInventory.addItem(LoreGuiItem.build(CivColor.LightBlue+ChatColor.BOLD+"Explore", ItemManager.getId(Material.COMPASS), 0, 
					ChatColor.RESET+"Venture outward from spawn into the wild",
					ChatColor.RESET+"and find a spot to settle. You may encounter",
					ChatColor.RESET+"trade resources, and other player towns which",
					ChatColor.RESET+"will infulence your decision on where to settle.",
					ChatColor.RESET+"Different biomes generate different resources."
					));
			
			tutorialInventory.addItem(LoreGuiItem.build(CivColor.LightBlue+ChatColor.BOLD+"Resources and Materials", ItemManager.getId(Material.DIAMOND_ORE), 0, 
					ChatColor.RESET+"CivCraft contains many new custom items.",
					ChatColor.RESET+"These items are crafted using a crafting bench",
					ChatColor.RESET+"and combining many more normal Minecraft items",
					ChatColor.RESET+"into higher tier items. Certain items like iron, gold,",
					ChatColor.RESET+"diamonds and emeralds can be exchanged for coins at "+CivColor.Yellow+"Bank",
					ChatColor.RESET+"structures. Coins can be traded for materials at the "+CivColor.Yellow+"Market"
					));
			
			tutorialInventory.addItem(LoreGuiItem.build(CivColor.LightBlue+ChatColor.BOLD+"Towns", ItemManager.getId(Material.FENCE), 0, 
					ChatColor.RESET+"Towns can be created by players to protect",
					ChatColor.RESET+"areas from outsiders. Inside a town the owners are",
					ChatColor.RESET+"free to build creatively without interference from griefers",
					ChatColor.RESET+"Towns cost materials to create and coins to maintain.",
					ChatColor.RESET+"Towns can build functional structures which allow it's",
					ChatColor.RESET+"residents access to more features. Towns can only be built",
					ChatColor.RESET+"inside of a civilization."
					));
			
			tutorialInventory.addItem(LoreGuiItem.build(CivColor.LightBlue+ChatColor.BOLD+"Civilizations", ItemManager.getId(Material.GOLD_HELMET), 0, 
					ChatColor.RESET+"Civilizations are collections of towns",
					ChatColor.RESET+"All towns inside of the civilization share technology",
					ChatColor.RESET+"which is researched by the civ. Many items and structures",
					ChatColor.RESET+"in CivCraft are only obtainable through the use of technology",
					ChatColor.RESET+"Founding your own civ is a lot of work, you must be a natural",
					ChatColor.RESET+"leader and bring people together in order for your civ to survive",
					ChatColor.RESET+"and flourish."
					));
			
			if (CivGlobal.isCasualMode()) {
				tutorialInventory.addItem(LoreGuiItem.build(CivColor.LightBlue+ChatColor.BOLD+"Casual War!", ItemManager.getId(Material.FIREWORK), 0, 
						ChatColor.RESET+"War allows civilizations to settle their differences.",
						ChatColor.RESET+"In casual mode, Civs have to the option to request war from",
						ChatColor.RESET+"each other. The winner of a war is awarded a trophy which can be",
						ChatColor.RESET+"displayed in an item frame for bragging rights.",
						ChatColor.RESET+"After a civilization is defeated in war, war must be requested again."
						));
			} else {
				tutorialInventory.addItem(LoreGuiItem.build(CivColor.LightBlue+ChatColor.BOLD+"War!", ItemManager.getId(Material.IRON_SWORD), 0, 
						ChatColor.RESET+"War allows civilizations to settle their differences.",
						ChatColor.RESET+"Normally, all structures inside a civilization are protected",
						ChatColor.RESET+"from damage. However civs have to the option to declare war on",
						ChatColor.RESET+"each other and do damage to each other's structures, and even capture",
						ChatColor.RESET+"towns from each other. Each weekend, WarTime is enabled for two hours",
						ChatColor.RESET+"during which players at war must defend their civ and conquer their enemies."
						));
			}
			
			tutorialInventory.setItem(8, LoreGuiItem.build(CivColor.LightBlue+ChatColor.BOLD+"More Info?", ItemManager.getId(Material.BOOK_AND_QUILL), 0, 
					ChatColor.RESET+"There is much more information you will require for your",
					ChatColor.RESET+"journey into CivCraft. Please visit the wiki at ",
					ChatColor.RESET+CivColor.LightGreen+ChatColor.BOLD+"http://civcraft.net/wiki",
					ChatColor.RESET+"For more detailed information about CivCraft and it's features."
					));
			
			tutorialInventory.setItem(9, LoreGuiItem.build(CivColor.LightBlue+ChatColor.BOLD+"QUEST: Build a Camp", ItemManager.getId(Material.BOOK_AND_QUILL), 0, 
					ChatColor.RESET+"First things first, in order to start your journey",
					ChatColor.RESET+"you must first build a camp. Camps allow you to store",
					ChatColor.RESET+"your materials safely, and allow you to obtain leadership",
					ChatColor.RESET+"tokens which can be crafted into a civ. The recipe for a camp is below."
					));
			
			tutorialInventory.setItem(18,getInfoBookForItem("mat_found_camp"));
			
			tutorialInventory.setItem(10, LoreGuiItem.build(CivColor.LightBlue+ChatColor.BOLD+"QUEST: Found a Civ", ItemManager.getId(Material.BOOK_AND_QUILL), 0, 
					ChatColor.RESET+"Next, you'll want to start a civilization.",
					ChatColor.RESET+"To do this, you must first obtain leadership tokens",
					ChatColor.RESET+"by feeding bread to your camp's longhouse.",
					ChatColor.RESET+"Once you have enough leadership tokens.",
					ChatColor.RESET+"You can craft the founding flag item below."
					));
			
			tutorialInventory.setItem(19,getInfoBookForItem("mat_found_civ"));
			
			tutorialInventory.setItem(11, LoreGuiItem.build(CivColor.LightBlue+ChatColor.BOLD+"Need to know a recipe?", ItemManager.getId(Material.WORKBENCH), 0, 
					ChatColor.RESET+"Type /res book to obtain the tutorial book",
					ChatColor.RESET+"and then click on 'Crafting Recipies'",
					ChatColor.RESET+"Every new item in CivCraft is listed here",
					ChatColor.RESET+"along with how to craft them.",
					ChatColor.RESET+"Good luck!"
					));
		
			LoreGuiItemListener.guiInventories.put(tutorialInventory.getName(), tutorialInventory);
		}
		
		if (player != null && player.isOnline() && player.isValid()) {
			player.openInventory(tutorialInventory);	
		}
	}
	
	public static ItemStack getInfoBookForItem(String matID) {
		LoreCraftableMaterial loreMat = LoreCraftableMaterial.getCraftMaterialFromId(matID);
		ItemStack stack = LoreMaterial.spawn(loreMat);
							
		if (!loreMat.isCraftable()) {
			return null;
		}
		
		AttributeUtil attrs = new AttributeUtil(stack);
		attrs.removeAll(); /* Remove all attribute modifiers to prevent them from displaying */
		LinkedList<String> lore = new LinkedList<String>();
		
		lore.add(""+ChatColor.RESET+ChatColor.BOLD+ChatColor.GOLD+"Click For Recipe");
		
		attrs.setLore(lore);				
		stack = attrs.getStack();
		return stack;
	}
	
	public static void showCraftingHelp(Player player) {
		if (craftingHelpInventory == null) {
			craftingHelpInventory = Bukkit.getServer().createInventory(player, MAX_CHEST_SIZE*9, "CivCraft Custom Item Recipes");

			/* Build the Category Inventory. */
			for (ConfigMaterialCategory cat : ConfigMaterialCategory.getCategories()) {
				if (cat.craftableCount == 0) {
					continue;
				}
				
				ItemStack infoRec = LoreGuiItem.build(cat.name, 
						ItemManager.getId(Material.WRITTEN_BOOK), 
						0, 
						CivColor.LightBlue+cat.materials.size()+" Items",
						CivColor.Gold+"<Click To Open>");
						infoRec = LoreGuiItem.setAction(infoRec, "OpenInventory");
						infoRec = LoreGuiItem.setActionData(infoRec, "invType", "showGuiInv");
						infoRec = LoreGuiItem.setActionData(infoRec, "invName", cat.name+" Recipes");
						
						craftingHelpInventory.addItem(infoRec);
						
						
				Inventory inv = Bukkit.createInventory(player, LoreGuiItem.MAX_INV_SIZE, cat.name+" Recipes");
				for (ConfigMaterial mat : cat.materials.values()) {
					ItemStack stack = getInfoBookForItem(mat.id);
					if (stack != null) {
						stack = LoreGuiItem.setAction(stack, "ShowRecipe");
						inv.addItem(LoreGuiItem.asGuiItem(stack));
					}
				}
				
				/* Add back buttons. */
				ItemStack backButton = LoreGuiItem.build("Back", ItemManager.getId(Material.MAP), 0, "Back to Categories");
				backButton = LoreGuiItem.setAction(backButton, "OpenInventory");
				backButton = LoreGuiItem.setActionData(backButton, "invType", "showCraftingHelp");
				inv.setItem(LoreGuiItem.MAX_INV_SIZE-1, backButton);
				
				LoreGuiItemListener.guiInventories.put(inv.getName(), inv);
			}
			
			LoreGuiItemListener.guiInventories.put(craftingHelpInventory.getName(), craftingHelpInventory);
		}
		
		player.openInventory(craftingHelpInventory);
	}
	
	public static void spawnGuiBook(Player player) {
		if (guiInventory == null) {
			guiInventory = Bukkit.getServer().createInventory(player, 3*9, "CivCraft Information");

			ItemStack infoRec = LoreGuiItem.build("CivCraft Info", 
					ItemManager.getId(Material.WRITTEN_BOOK), 
							0, CivColor.Gold+"<Click To View>");
			infoRec = LoreGuiItem.setAction(infoRec, "OpenInventory");
			infoRec = LoreGuiItem.setActionData(infoRec, "invType", "showTutorialInventory");
			guiInventory.addItem(infoRec);
			
			ItemStack craftRec = LoreGuiItem.build("Crafting Recipes", 
					ItemManager.getId(Material.WRITTEN_BOOK), 
					0, CivColor.Gold+"<Click To View>");
			craftRec = LoreGuiItem.setAction(craftRec, "OpenInventory");
			craftRec = LoreGuiItem.setActionData(craftRec, "invType", "showCraftingHelp");
			guiInventory.addItem(craftRec);
			
			ItemStack buildMenu = LoreGuiItem.build("Build Structure", ItemManager.getId(Material.BRICK_STAIRS), 0, CivColor.Gold+"<Click to View>");
			buildMenu = LoreGuiItem.setAction(buildMenu, "BuildStructureList");
			guiInventory.addItem(buildMenu);
			
			
			LoreGuiItemListener.guiInventories.put(guiInventory.getName(), guiInventory);
		}
		
		player.openInventory(guiInventory);

	}
	
	
}
