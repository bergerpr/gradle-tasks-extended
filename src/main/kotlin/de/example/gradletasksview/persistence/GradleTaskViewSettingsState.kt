package de.example.gradletasksview.persistence

import de.example.gradletasksview.model.GradleTaskViewMode

/** Persistierter Zustand pro Projekt. */
class GradleTaskViewSettingsState {
    var selectedMode: GradleTaskViewMode = GradleTaskViewMode.DEFAULT
}
