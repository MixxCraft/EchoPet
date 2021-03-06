/*
 * This file is part of EchoPet.
 *
 * EchoPet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * EchoPet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with EchoPet.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.dsh105.echopet.api;

import com.dsh105.dshutils.logger.Logger;
import com.dsh105.dshutils.util.EnumUtil;
import com.dsh105.dshutils.util.StringUtil;
import io.github.dsh105.echopet.compat.api.entity.*;
import io.github.dsh105.echopet.compat.api.entity.type.pet.*;
import io.github.dsh105.echopet.compat.api.plugin.EchoPet;
import io.github.dsh105.echopet.compat.api.plugin.IPetManager;
import io.github.dsh105.echopet.compat.api.plugin.PetStorage;
import io.github.dsh105.echopet.compat.api.util.Lang;
import io.github.dsh105.echopet.compat.api.util.WorldUtil;
import io.github.dsh105.echopet.compat.api.util.PetUtil;
import org.bukkit.DyeColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager.Profession;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;


public class PetManager implements IPetManager {

    private ArrayList<IPet> pets = new ArrayList<IPet>();

    @Override
    public ArrayList<IPet> getPets() {
        return pets;
    }

    @Override
    public IPet loadPets(Player p, boolean findDefault, boolean sendMessage, boolean checkWorldOverride) {
        if (EchoPet.getOptions().sqlOverride()) {
            IPet pet = EchoPet.getSqlManager().createPetFromDatabase(p.getName());
            if (pet == null) {
                return null;
            } else {
                if (sendMessage) {
                    Lang.sendTo(p, Lang.DATABASE_PET_LOAD.toString().replace("%petname%", pet.getPetName().toString()));
                }
            }
            return pet;
        } else if (EchoPet.getConfig(EchoPet.ConfigType.DATA).get("default." + p.getName() + ".pet.type") != null && findDefault) {
            IPet pi = this.createPetFromFile("default", p);
            if (pi == null) {
                return null;
            } else {
                if (sendMessage) {
                    Lang.sendTo(p, Lang.DEFAULT_PET_LOAD.toString().replace("%petname%", pi.getPetName().toString()));
                }
            }
            return pi;
        } else if ((checkWorldOverride && EchoPet.getOptions().getConfig().getBoolean("multiworldLoadOverride", true)) || EchoPet.getOptions().getConfig().getBoolean("loadSavedPets", true)) {
            if (EchoPet.getConfig(EchoPet.ConfigType.DATA).get("autosave." + p.getName() + ".pet.type") != null) {
                IPet pi = this.createPetFromFile("autosave", p);
                if (pi == null) {
                    return null;
                } else {
                    if (sendMessage) {
                        Lang.sendTo(p, Lang.AUTOSAVE_PET_LOAD.toString().replace("%petname%", pi.getPetName().toString()));
                    }
                }
                return pi;
            }
        }
        return null;
    }

    @Override
    public void removeAllPets() {
        Iterator<IPet> i = pets.listIterator();
        while (i.hasNext()) {
            IPet p = i.next();
            saveFileData("autosave", p);
            EchoPet.getSqlManager().saveToDatabase(p, false);
            p.removePet(true);
            i.remove();
        }
    }

    @Override
    public IPet createPet(Player owner, PetType petType, boolean sendMessageOnFail) {
        removePets(owner.getName(), true);
        if (!WorldUtil.allowPets(owner.getLocation())) {
            if (sendMessageOnFail) {
                Lang.sendTo(owner, Lang.PETS_DISABLED_HERE.toString().replace("%world%", StringUtil.capitalise(owner.getWorld().getName())));
            }
            return null;
        }
        if (!EchoPet.getOptions().allowPetType(petType)) {
            if (sendMessageOnFail) {
                Lang.sendTo(owner, Lang.PET_TYPE_DISABLED.toString().replace("%type%", StringUtil.capitalise(petType.toString())));
            }
            return null;
        }
        IPet pi = petType.getNewPetInstance(owner);
        //Pet pi = new Pet(owner, petType);
        forceAllValidData(pi);
        pets.add(pi);
        //saveFileData("autosave", pi);
        //plugin.SPH.saveToDatabase(pi, false);
        return pi;
    }

    @Override
    public IPet createPet(Player owner, PetType petType, PetType riderType) {
        removePets(owner.getName(), true);
        if (!WorldUtil.allowPets(owner.getLocation())) {
            Lang.sendTo(owner, Lang.PETS_DISABLED_HERE.toString().replace("%world%", StringUtil.capitalise(owner.getWorld().getName())));
            return null;
        }
        if (!EchoPet.getOptions().allowPetType(petType)) {
            Lang.sendTo(owner, Lang.PET_TYPE_DISABLED.toString().replace("%type%", StringUtil.capitalise(petType.toString())));
            return null;
        }
        IPet pi = petType.getNewPetInstance(owner);
        //Pet pi = new Pet(owner, petType);
        pi.createRider(riderType, true);
        forceAllValidData(pi);
        pets.add(pi);
        //saveFileData("autosave", pi);
        //plugin.SPH.saveToDatabase(pi, false);
        return pi;
    }

    @Override
    public IPet getPet(Player player) {
        for (IPet pi : pets) {
            if (pi.getNameOfOwner().equals(player.getName())) {
                return pi;
            }
        }
        return null;
    }

    @Override
    public IPet getPet(Entity pet) {
        for (IPet pi : pets) {
            if (pi.getEntityPet().equals(pet) || pi.getRider().getEntityPet().equals(pet)) {
                return pi;
            }
            if (pi.getCraftPet().equals(pet) || pi.getRider().getCraftPet().equals(pet)) {
                return pi;
            }
        }
        return null;
    }

    // *May* add support for multiple pets
    /*public List<Pet> getPets(Player player) {
        List<Pet> tempList = new ArrayList<Pet>();
		
		for (Pet pi : pets) {
			if (pi.getPlayerOwner() == player) tempList.add(pi);
		}
		
		return tempList.isEmpty() ? null : tempList;
	}
	
	public int getMaxPets() {
		return plugin.getConfig().getInt("maxPets");
	}*/

    // Force all data specified in config file and notify player.
    @Override
    public void forceAllValidData(IPet pi) {
        ArrayList<PetData> tempData = new ArrayList<PetData>();
        for (PetData data : PetData.values()) {
            if (EchoPet.getOptions().forceData(pi.getPetType(), data)) {
                tempData.add(data);
            }
        }
        setData(pi, tempData.toArray(new PetData[tempData.size()]), true);

        ArrayList<PetData> tempRiderData = new ArrayList<PetData>();
        if (pi.getRider() != null) {
            for (PetData data : PetData.values()) {
                if (EchoPet.getOptions().forceData(pi.getPetType(), data)) {
                    tempRiderData.add(data);
                }
            }
            setData(pi.getRider(), tempRiderData.toArray(new PetData[tempData.size()]), true);
        }

        if (EchoPet.getOptions().getConfig().getBoolean("sendForceMessage", true)) {
            String dataToString = " ";
            if (!tempRiderData.isEmpty()) {
                dataToString = PetUtil.dataToString(tempData);
            } else {
                dataToString = PetUtil.dataToString(tempData, tempRiderData);
            }
            if (dataToString != " ") {
                Lang.sendTo(pi.getOwner(), Lang.DATA_FORCE_MESSAGE.toString().replace("%data%", dataToString));
            }
        }
    }

    @Override
    public void updateFileData(String type, IPet pet, ArrayList<PetData> list, boolean b) {
        EchoPet.getSqlManager().updateDatabase(pet.getNameOfOwner(), list, b, pet.isRider());
        String w = pet.getOwner().getWorld().getName();
        String path = type + "." + w + "." + pet.getNameOfOwner();
        for (PetData pd : list) {
            EchoPet.getConfig(EchoPet.ConfigType.DATA).set(path + ".pet.data." + pd.toString().toLowerCase(), b);
        }
        EchoPet.getConfig(EchoPet.ConfigType.DATA).saveConfig();
    }

    @Override
    public IPet createPetFromFile(String type, Player p) {
        if (EchoPet.getOptions().getConfig().getBoolean("loadSavedPets", true)) {
            String path = type + "." + p.getName();
            if (EchoPet.getConfig(EchoPet.ConfigType.DATA).get(path) != null) {
                PetType petType = PetType.valueOf(EchoPet.getConfig(EchoPet.ConfigType.DATA).getString(path + ".pet.type"));
                String name = EchoPet.getConfig(EchoPet.ConfigType.DATA).getString(path + ".pet.name");
                if (name.equalsIgnoreCase("") || name == null) {
                    name = petType.getDefaultName(p.getName());
                }
                if (petType == null) return null;
                if (!EchoPet.getOptions().allowPetType(petType)) {
                    return null;
                }
                IPet pi = this.createPet(p, petType, true);
                if (pi == null) {
                    return null;
                }
                //Pet pi = petType.getNewPetInstance(p, petType);
                //Pet pi = new Pet(p, petType);

                pi.setPetName(name);

                ArrayList<PetData> data = new ArrayList<PetData>();
                ConfigurationSection cs = EchoPet.getConfig(EchoPet.ConfigType.DATA).getConfigurationSection(path + ".pet.data");
                if (cs != null) {
                    for (String key : cs.getKeys(false)) {
                        if (EnumUtil.isEnumType(PetData.class, key.toUpperCase())) {
                            PetData pd = PetData.valueOf(key.toUpperCase());
                            data.add(pd);
                        } else {
                            Logger.log(Logger.LogLevel.WARNING, "Error whilst loading data Pet Save Data for " + pi.getNameOfOwner() + ". Unknown enum type: " + key + ".", true);
                        }
                    }
                }

                if (!data.isEmpty()) {
                    setData(pi, data.toArray(new PetData[data.size()]), true);
                }

                this.loadRiderFromFile(type, pi);

                forceAllValidData(pi);
                return pi;
            }
        }
        return null;
    }

    @Override
    public void loadRiderFromFile(IPet pet) {
        this.loadRiderFromFile("autosave", pet);
    }

    @Override
    public void loadRiderFromFile(String type, IPet pet) {
        if (pet.getNameOfOwner() != null && pet.getOwner() != null) {
            String path = type + "." + pet.getNameOfOwner();
            if (EchoPet.getConfig(EchoPet.ConfigType.DATA).get(path + ".rider.type") != null) {
                PetType riderPetType = PetType.valueOf(EchoPet.getConfig(EchoPet.ConfigType.DATA).getString(path + ".rider.type"));
                String riderName = EchoPet.getConfig(EchoPet.ConfigType.DATA).getString(path + ".rider.name");
                if (riderName.equalsIgnoreCase("") || riderName == null) {
                    riderName = riderPetType.getDefaultName(pet.getNameOfOwner());
                }
                if (riderPetType == null) return;
                if (EchoPet.getOptions().allowRidersFor(pet.getPetType())) {
                    IPet rider = pet.createRider(riderPetType, true);
                    if (rider != null && rider.getEntityPet() != null) {
                        rider.setPetName(riderName);
                        ArrayList<PetData> riderData = new ArrayList<PetData>();
                        ConfigurationSection mcs = EchoPet.getConfig(EchoPet.ConfigType.DATA).getConfigurationSection(path + ".rider.data");
                        if (mcs != null) {
                            for (String key : mcs.getKeys(false)) {
                                if (EnumUtil.isEnumType(PetData.class, key.toUpperCase())) {
                                    PetData pd = PetData.valueOf(key.toUpperCase());
                                    riderData.add(pd);
                                } else {
                                    Logger.log(Logger.LogLevel.WARNING, "Error whilst loading data Pet Rider Save Data for " + pet.getNameOfOwner() + ". Unknown enum type: " + key + ".", true);
                                }
                            }
                        }
                        if (!riderData.isEmpty()) {
                            setData(pet, riderData.toArray(new PetData[riderData.size()]), true);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void removePets(String player, boolean makeDeathSound) {
        Iterator<IPet> i = pets.listIterator();
        while (i.hasNext()) {
            IPet p = i.next();
            if (p.getNameOfOwner().equals(player)) {
                p.removePet(makeDeathSound);
                i.remove();
            }
        }
    }

    @Override
    public void removePet(IPet pi, boolean makeDeathSound) {
        removePets(pi.getNameOfOwner(), makeDeathSound);
    }

    @Override
    public void saveFileData(String type, IPet pi) {
        clearFileData(type, pi);
        String oName = pi.getNameOfOwner();
        String path = type + "." + oName;
        PetType petType = pi.getPetType();

        EchoPet.getConfig(EchoPet.ConfigType.DATA).set(path + ".pet.type", petType.toString());
        EchoPet.getConfig(EchoPet.ConfigType.DATA).set(path + ".pet.name", pi.getPetNameWithoutColours());

        for (PetData pd : pi.getPetData()) {
            EchoPet.getConfig(EchoPet.ConfigType.DATA).set(path + ".pet.data." + pd.toString().toLowerCase(), true);
        }

        if (pi.getRider() != null) {
            PetType riderType = pi.getRider().getPetType();

            EchoPet.getConfig(EchoPet.ConfigType.DATA).set(path + ".rider.type", riderType.toString());
            EchoPet.getConfig(EchoPet.ConfigType.DATA).set(path + ".rider.name", pi.getRider().getPetNameWithoutColours());
            for (PetData pd : pi.getRider().getPetData()) {
                EchoPet.getConfig(EchoPet.ConfigType.DATA).set(path + ".rider.data." + pd.toString().toLowerCase(), true);
            }
        }
        EchoPet.getConfig(EchoPet.ConfigType.DATA).saveConfig();
    }

    @Override
    public void saveFileData(String type, Player p, PetStorage UPD, PetStorage UMD) {
        this.saveFileData(type, p.getName(), UPD, UMD);
    }

    @Override
    public void saveFileData(String type, String name, PetStorage UPD, PetStorage UMD) {
        clearFileData(type, name);
        PetType pt = UPD.petType;
        PetData[] data = UPD.petDataList.toArray(new PetData[UPD.petDataList.size()]);
        String petName = UPD.petName;
        if (UPD.petName == null || UPD.petName.equalsIgnoreCase("")) {
            petName = pt.getDefaultName(name);
        }
        PetType riderType = UMD.petType;
        PetData[] riderData = UMD.petDataList.toArray(new PetData[UMD.petDataList.size()]);
        String riderName = UMD.petName;
        if (UMD.petName == null || UMD.petName.equalsIgnoreCase("")) {
            riderName = pt.getDefaultName(name);
        }

        String path = type + "." + name;
        try {
            EchoPet.getConfig(EchoPet.ConfigType.DATA).set(path + ".pet.type", pt.toString());
            EchoPet.getConfig(EchoPet.ConfigType.DATA).set(path + ".pet.name", petName);

            for (PetData pd : data) {
                EchoPet.getConfig(EchoPet.ConfigType.DATA).set(path + ".pet.data." + pd.toString().toLowerCase(), true);
            }

            if (riderData != null && riderType != null) {
                EchoPet.getConfig(EchoPet.ConfigType.DATA).set(path + ".rider.type", riderType.toString());
                EchoPet.getConfig(EchoPet.ConfigType.DATA).set(path + ".rider.name", riderName);
                for (PetData pd : riderData) {
                    EchoPet.getConfig(EchoPet.ConfigType.DATA).set(path + ".rider.data." + pd.toString().toLowerCase(), true);
                }

            }
        } catch (Exception e) {
        }
        EchoPet.getConfig(EchoPet.ConfigType.DATA).saveConfig();
    }

    @Override
    public void saveFileData(String type, Player p, PetStorage UPD) {
        this.saveFileData(type, p.getName(), UPD);
    }

    @Override
    public void saveFileData(String type, String name, PetStorage UPD) {
        clearFileData(type, name);
        PetType pt = UPD.petType;
        PetData[] data = UPD.petDataList.toArray(new PetData[UPD.petDataList.size()]);
        String petName = UPD.petName;

        String path = type + "." + name;
        try {
            EchoPet.getConfig(EchoPet.ConfigType.DATA).set(path + ".pet.type", pt.toString());
            EchoPet.getConfig(EchoPet.ConfigType.DATA).set(path + ".pet.name", petName);

            for (PetData pd : data) {
                EchoPet.getConfig(EchoPet.ConfigType.DATA).set(path + ".pet.data." + pd.toString().toLowerCase(), true);
            }

        } catch (Exception e) {
        }
        EchoPet.getConfig(EchoPet.ConfigType.DATA).saveConfig();
    }

    @Override
    public void clearAllFileData() {
        for (String key : EchoPet.getConfig(EchoPet.ConfigType.DATA).getKeys(true)) {
            if (EchoPet.getConfig(EchoPet.ConfigType.DATA).get(key) != null) {
                EchoPet.getConfig(EchoPet.ConfigType.DATA).set(key, null);
            }
        }
        EchoPet.getConfig(EchoPet.ConfigType.DATA).saveConfig();
    }

    @Override
    public void clearFileData(String type, IPet pi) {
        clearFileData(type, pi.getNameOfOwner());
    }

    @Override
    public void clearFileData(String type, String pName) {
        String path = type + "." + pName;
        EchoPet.getConfig(EchoPet.ConfigType.DATA).set(path, null);
        /*if (EchoPet.getConfig(EchoPet.ConfigType.DATA).get(path + ".pet.type") != null) {
            for (String key : EchoPet.getConfig(EchoPet.ConfigType.DATA).getConfigurationSection(path).getKeys(false)) {
				for (String key1 : EchoPet.getConfig(EchoPet.ConfigType.DATA).getConfigurationSection(path + "." + key).getKeys(false)) {
					if (EchoPet.getConfig(EchoPet.ConfigType.DATA).get(path + "." + key + "." + key1) != null) {
						EchoPet.getConfig(EchoPet.ConfigType.DATA).set(path + "." + key + "." + key1, null);
					}
				}
				if (EchoPet.getConfig(EchoPet.ConfigType.DATA).get(path + "." + key) != null) {
					EchoPet.getConfig(EchoPet.ConfigType.DATA).set(path + "." + key, null);
				}
			}
		}*/
        EchoPet.getConfig(EchoPet.ConfigType.DATA).saveConfig();
    }

    @Override
    public void clearFileData(String type, Player p) {
        clearFileData(type, p.getName());
    }

    @Override
    public void setData(IPet pet, PetData[] data, boolean b) {
        PetType petType = pet.getPetType();
        for (PetData pd : data) {
            setData(pet, pd, b);
        }
    }

    @Override
    public void setData(IPet pet, PetData pd, boolean b) {
        PetType petType = pet.getPetType();
        if (petType.isDataAllowed(pd)) {
            if (pd == PetData.BABY) {
                if (petType == PetType.ZOMBIE) {
                    ((IZombiePet) pet).setBaby(b);
                } else if (petType == PetType.PIGZOMBIE) {
                    ((IPigZombiePet) pet).setBaby(b);
                } else {
                    ((IAgeablePet) pet).setBaby(b);
                }
            }

            if (pd == PetData.POWER) {
                ((ICreeperPet) pet).setPowered(b);
            }

            if (pd.isType(PetData.Type.SIZE)) {
                int i = 1;
                if (pd == PetData.MEDIUM) {
                    i = 2;
                } else if (pd == PetData.LARGE) {
                    i = 4;
                }
                if (petType == PetType.SLIME) {
                    ((ISlimePet) pet).setSize(i);
                }
                if (petType == PetType.MAGMACUBE) {
                    ((IMagmaCubePet) pet).setSize(i);
                }
            }

            if (pd.isType(PetData.Type.CAT) && petType == PetType.OCELOT) {
                try {
                    org.bukkit.entity.Ocelot.Type t = org.bukkit.entity.Ocelot.Type.valueOf(pd.toString() + (pd == PetData.WILD ? "_OCELOT" : "_CAT"));
                    if (t != null) {
                        ((IOcelotPet) pet).setCatType(t);
                    }
                } catch (Exception e) {
                    Logger.log(Logger.LogLevel.SEVERE, "Encountered exception whilst attempting to convert PetData to Ocelot.Type.", e, true);
                }
            }

            if (pd == PetData.ANGRY) {
                ((IWolfPet) pet).setAngry(b);
            }

            if (pd == PetData.TAMED) {
                ((IWolfPet) pet).setTamed(b);
            }

            if (pd.isType(PetData.Type.PROF)) {
                Profession p = Profession.valueOf(pd.toString());
                if (p != null) {
                    ((IVillagerPet) pet).setProfession(p);
                }
            }

            if (pd.isType(PetData.Type.COLOUR) && (petType == PetType.SHEEP || petType == PetType.WOLF)) {
                String s = pd == PetData.LIGHTBLUE ? "LIGHT_BLUE" : pd.toString();
                try {
                    DyeColor dc = DyeColor.valueOf(s);
                    if (dc != null) {
                        if (petType == PetType.SHEEP) {
                            ((ISheepPet) pet).setColor(dc);
                        } else if (petType == PetType.WOLF) {
                            ((IWolfPet) pet).setCollarColor(dc);
                        }
                    }
                } catch (Exception e) {
                    Logger.log(Logger.LogLevel.SEVERE, "Encountered exception whilst attempting to convert PetData to DyeColor.", e, true);
                }
            }

            if (pd == PetData.WITHER) {
                ((ISkeletonPet) pet).setWither(b);
            }

            if (pd == PetData.VILLAGER) {
                if (petType == PetType.ZOMBIE) {
                    ((IZombiePet) pet).setVillager(b);
                } else if (petType == PetType.PIGZOMBIE) {
                    ((IPigZombiePet) pet).setVillager(b);
                }
            }

            if (pd == PetData.FIRE) {
                ((IBlazePet) pet).setOnFire(b);
            }

            if (pd == PetData.SADDLE) {
                if (petType == PetType.PIG) {
                    ((IPigPet) pet).setSaddle(b);
                } else if (petType == PetType.HORSE) {
                    ((IHorsePet) pet).setSaddled(b);
                }
            }

            if (pd == PetData.SHEARED) {
                ((ISheepPet) pet).setSheared(b);
            }

            if (pd == PetData.SCREAMING) {
                ((IEndermanPet) pet).setScreaming(b);
            }

            if (pd == PetData.SHIELD) {
                ((IWitherPet) pet).setShielded(b);
            }


            if (petType == PetType.HORSE) {
                if (pd == PetData.CHESTED) {
                    ((IHorsePet) pet).setChested(b);
                }

                if (pd.isType(PetData.Type.HORSE_TYPE)) {
                    try {
                        HorseType h = HorseType.valueOf(pd.toString());
                        if (h != null) {
                            ((IHorsePet) pet).setHorseType(h);
                        }
                    } catch (Exception e) {
                        Logger.log(Logger.LogLevel.WARNING, "Encountered exception whilst attempting to convert PetData to Horse.Type.", e, true);
                    }
                }

                if (pd.isType(PetData.Type.HORSE_VARIANT)) {
                    try {
                        HorseVariant v = HorseVariant.valueOf(pd.toString());
                        if (v != null) {
                            HorseMarking m = ((IHorsePet) pet).getMarking();
                            if (m == null) {
                                m = HorseMarking.NONE;
                            }
                            ((IHorsePet) pet).setVariant(v, m);
                        }
                    } catch (Exception e) {
                        Logger.log(Logger.LogLevel.WARNING, "Encountered exception whilst attempting to convert PetData to Horse.Variant.", e, true);
                    }
                }

                if (pd.isType(PetData.Type.HORSE_MARKING)) {
                    try {
                        HorseMarking m;
                        if (pd == PetData.WHITEPATCH) {
                            m = HorseMarking.WHITE_PATCH;
                        } else if (pd == PetData.WHITESPOT) {
                            m = HorseMarking.WHITE_SPOTS;
                        } else if (pd == PetData.BLACKSPOT) {
                            m = HorseMarking.BLACK_SPOTS;
                        } else {
                            m = HorseMarking.valueOf(pd.toString());
                        }
                        if (m != null) {
                            HorseVariant v = ((IHorsePet) pet).getVariant();
                            if (v == null) {
                                v = HorseVariant.WHITE;
                            }
                            ((IHorsePet) pet).setVariant(v, m);
                        }
                    } catch (Exception e) {
                        Logger.log(Logger.LogLevel.WARNING, "Encountered exception whilst attempting to convert PetData to Horse.Marking.", e, true);
                    }
                }

                if (pd.isType(PetData.Type.HORSE_ARMOUR)) {
                    try {
                        HorseArmour a;
                        if (pd == PetData.NOARMOUR) {
                            a = HorseArmour.NONE;
                        } else {
                            a = HorseArmour.valueOf(pd.toString());
                        }
                        if (a != null) {
                            ((IHorsePet) pet).setArmour(a);
                        }
                    } catch (Exception e) {
                        Logger.log(Logger.LogLevel.WARNING, "Encountered exception whilst attempting to convert PetData to Horse.Armour.", e, true);
                    }
                }
            }
            ListIterator<PetData> i = pet.getPetData().listIterator();
            while (i.hasNext()) {
                PetData petData = i.next();
                if (petData != pd) {
                    ListIterator<PetData.Type> i2 = pd.getTypes().listIterator();
                    while (i2.hasNext()) {
                        PetData.Type type = i2.next();
                        if (type != PetData.Type.BOOLEAN && petData.isType(type)) {
                            i.remove();
                            break;
                        }
                    }
                }
            }

            if (b) {
                if (!pet.getPetData().contains(pd)) {
                    pet.getPetData().add(pd);
                }
            } else {
                if (pet.getPetData().contains(pd)) {
                    pet.getPetData().remove(pd);
                }
            }
        }
    }
}