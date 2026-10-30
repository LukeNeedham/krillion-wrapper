package com.lukeneedham.krillionwrapper.di

import com.lukeneedham.krillionwrapper.game.GameViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** The app's Koin bindings: one view model per page. */
object KoinModules {
    private val app = module {
        viewModel { GameViewModel(get()) }
    }

    val modules: List<Module> = listOf(app)
}
