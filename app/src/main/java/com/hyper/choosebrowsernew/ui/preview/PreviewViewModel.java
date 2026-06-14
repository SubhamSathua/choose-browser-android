package com.hyper.choosebrowsernew.ui.preview;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.hyper.choosebrowsernew.data.repository.PreviewRepository;

public class PreviewViewModel extends ViewModel {

    private final PreviewRepository repository;

    private final MutableLiveData<Boolean> _jsEnabled = new MutableLiveData<>();
    public final LiveData<Boolean> jsEnabled = _jsEnabled;

    private final MutableLiveData<String> _adBlockMode = new MutableLiveData<>();
    public final LiveData<String> adBlockMode = _adBlockMode;

    private final MutableLiveData<Integer> _blockedCount = new MutableLiveData<>(0);
    public final LiveData<Integer> blockedCount = _blockedCount;

    private boolean allowJsEnabled;

    public PreviewViewModel(PreviewRepository repository) {
        this.repository = repository;
        loadSettings();
    }

    private void loadSettings() {
        allowJsEnabled = repository.isAllowJsEnabled();
        _jsEnabled.setValue(allowJsEnabled);
        _adBlockMode.setValue(repository.getAdBlockMode());
    }

    public void toggleJs() {
        boolean current = _jsEnabled.getValue() != null && _jsEnabled.getValue();
        _jsEnabled.setValue(!current);
    }

    public void setPersistentJs(boolean enabled) {
        repository.setAllowJsEnabled(enabled);
        allowJsEnabled = enabled;
        _jsEnabled.setValue(enabled);
    }

    public void setAdBlockMode(String mode) {
        repository.setAdBlockMode(mode);
        _adBlockMode.setValue(mode);
    }

    public void incrementBlockedCount() {
        Integer current = _blockedCount.getValue();
        _blockedCount.postValue((current != null ? current : 0) + 1);
    }

    public void resetBlockedCount() {
        _blockedCount.setValue(0);
    }

    public boolean shouldBlock(String host) {
        return repository.shouldBlock(host, _adBlockMode.getValue());
    }
}
