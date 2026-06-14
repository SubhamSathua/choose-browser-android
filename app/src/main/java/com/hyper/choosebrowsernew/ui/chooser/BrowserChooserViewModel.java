package com.hyper.choosebrowsernew.ui.chooser;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.hyper.choosebrowsernew.data.model.AppInfo;
import com.hyper.choosebrowsernew.data.repository.BrowserRepository;

import java.util.List;

public class BrowserChooserViewModel extends ViewModel {

    private final BrowserRepository repository;

    private final MutableLiveData<List<AppInfo>> _browsers = new MutableLiveData<>();
    public final LiveData<List<AppInfo>> browsers = _browsers;

    public BrowserChooserViewModel(BrowserRepository repository) {
        this.repository = repository;
    }

    public void loadBrowsers() {
        _browsers.setValue(repository.getInstalledBrowsers());
    }

    public List<String> findUrls(String text) {
        return repository.extractUrls(text);
    }
}
