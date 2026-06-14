package com.hyper.choosebrowsernew.ui.common;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.hyper.choosebrowsernew.data.repository.BrowserRepository;
import com.hyper.choosebrowsernew.data.repository.PreviewRepository;
import com.hyper.choosebrowsernew.data.repository.SettingsRepository;
import com.hyper.choosebrowsernew.data.repository.UpdateRepository;
import com.hyper.choosebrowsernew.ui.chooser.BrowserChooserViewModel;
import com.hyper.choosebrowsernew.ui.main.MainViewModel;
import com.hyper.choosebrowsernew.ui.preview.PreviewViewModel;
import com.hyper.choosebrowsernew.ui.settings.SettingsViewModel;

public class ViewModelFactory implements ViewModelProvider.Factory {

    private final Context context;

    public ViewModelFactory(Context context) {
        this.context = context.getApplicationContext();
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(SettingsViewModel.class)) {
            return (T) new SettingsViewModel(new SettingsRepository(context));
        }
        if (modelClass.isAssignableFrom(MainViewModel.class)) {
            return (T) new MainViewModel(new UpdateRepository(context));
        }
        if (modelClass.isAssignableFrom(BrowserChooserViewModel.class)) {
            return (T) new BrowserChooserViewModel(new BrowserRepository(context));
        }
        if (modelClass.isAssignableFrom(PreviewViewModel.class)) {
            return (T) new PreviewViewModel(new PreviewRepository(context));
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
