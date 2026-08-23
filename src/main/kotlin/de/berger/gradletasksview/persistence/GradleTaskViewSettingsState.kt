package de.berger.gradletasksview.persistence

import de.berger.gradletasksview.model.GradleTaskViewMode

/** Persistierter Zustand pro Projekt. */
class GradleTaskViewSettingsState {
    var selectedMode: GradleTaskViewMode = GradleTaskViewMode.DEFAULT
}
