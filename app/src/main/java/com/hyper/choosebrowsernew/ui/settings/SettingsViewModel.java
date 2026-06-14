package com.hyper.choosebrowsernew.ui.settings;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.hyper.choosebrowsernew.data.repository.SettingsRepository;

public class SettingsViewModel extends ViewModel {

    private final SettingsRepository repository;

    private final MutableLiveData<Integer> _themeMode = new MutableLiveData<>();
    public final LiveData<Integer> themeMode = _themeMode;

    private final MutableLiveData<String> _colorThemeId = new MutableLiveData<>();
    public final LiveData<String> colorThemeId = _colorThemeId;

    private final MutableLiveData<String> _appVersion = new MutableLiveData<>();
    public final LiveData<String> appVersion = _appVersion;

    public SettingsViewModel(SettingsRepository repository) {
        this.repository = repository;
        loadSettings();
    }

    private void loadSettings() {
        _themeMode.setValue(repository.getThemeMode());
        _colorThemeId.setValue(repository.getColorThemeId());
        _appVersion.setValue("Version " + repository.getAppVersion());
    }

    public void setThemeMode(int mode) {
        repository.setThemeMode(mode);
        _themeMode.setValue(mode);
    }

    public void setColorThemeId(String themeId) {
        repository.setColorThemeId(themeId);
        _colorThemeId.setValue(themeId);
    }
}
