package com.lukeneedham.krillionwrapper.di

import com.lukeneedham.krillionwrapper.game.GameViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** The app's Koin bindings: one view model per page. */
val appModule = module {
    viewModel { GameViewModel(get()) }
}
