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

package io.github.dsh105.echopet.api.pet.type;

import io.github.dsh105.echopet.api.pet.Pet;
import io.github.dsh105.echopet.compat.api.entity.EntityPetType;
import io.github.dsh105.echopet.compat.api.entity.IEntityPet;
import io.github.dsh105.echopet.compat.api.entity.PetType;
import io.github.dsh105.echopet.compat.api.entity.type.nms.IEntityBlazePet;
import io.github.dsh105.echopet.compat.api.entity.type.pet.IBlazePet;
import org.bukkit.entity.Player;

@EntityPetType(petType = PetType.BLAZE)
public class BlazePet extends Pet implements IBlazePet {

    boolean onFire;

    public BlazePet(Player owner) {
        super(owner);
    }

    public BlazePet(String owner, IEntityPet entityPet) {
        super(owner, entityPet);
    }

    @Override
    public void setOnFire(boolean flag) {
        ((IEntityBlazePet) getEntityPet()).setOnFire(flag);
        this.onFire = flag;
    }

    @Override
    public boolean isOnFire() {
        return this.onFire;
    }
}
