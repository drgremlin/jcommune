/**
 * Copyright (C) 2011  JTalks.org Team
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.jtalks.jcommune.model.entity;

import org.jtalks.common.model.entity.Entity;

import java.util.List;

/**
 *
 * @author Anuar Nurmakanov
 */
public class PluginConfiguration extends Entity {
    private String name;
    private boolean active;
    private List<PluginProperty> properties;

    public PluginConfiguration() {
    }

    public PluginConfiguration(String name, boolean active, List<PluginProperty> properties) {
        this.name = name;
        this.active = active;
        this.properties = properties;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<PluginProperty> getProperties() {
        return properties;
    }

    public void setProperties(List<PluginProperty> properties) {
        this.properties = properties;
    }
}
