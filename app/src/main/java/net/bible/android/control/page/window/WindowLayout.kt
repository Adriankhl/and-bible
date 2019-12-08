/*
 * Copyright (c) 2018 Martin Denham, Tuomas Airaksinen and the And Bible contributors.
 *
 * This file is part of And Bible (http://github.com/AndBible/and-bible).
 *
 * And Bible is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * And Bible is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with And Bible.
 * If not, see http://www.gnu.org/licenses/.
 *
 */

package net.bible.android.control.page.window

import net.bible.android.database.workspaces.WorkspaceEntities
import org.json.JSONException
import org.json.JSONObject

class WindowLayout(entity: WorkspaceEntities.WindowLayout?) {

    var state =
        if(entity != null) WindowState.valueOf(entity.state) else WindowState.SPLIT

    var weight = entity?.weight ?: 1.0f


    val stateJson: JSONObject
        @Throws(JSONException::class)
        get() {
            val `object` = JSONObject()
            `object`.put("state", state.toString())
                    .put("weight", weight.toDouble())
            return `object`
        }

    enum class WindowState {
        SPLIT, MINIMISED, MAXIMISED, CLOSED
    }

    @Throws(JSONException::class)
    fun restoreState(jsonObject: JSONObject) {
        this.state = WindowState.valueOf(jsonObject.getString("state"))
        this.weight = jsonObject.getDouble("weight").toFloat()
    }

    fun restoreFrom(windowLayoutEntity: WorkspaceEntities.WindowLayout) {
        state = WindowLayout.WindowState.valueOf(windowLayoutEntity.state)
        weight = windowLayoutEntity.weight
    }
}
