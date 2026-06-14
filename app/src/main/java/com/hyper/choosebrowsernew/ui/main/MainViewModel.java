package com.hyper.choosebrowsernew.ui.main;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.hyper.choosebrowsernew.data.model.UpdateResult;
import com.hyper.choosebrowsernew.data.repository.UpdateRepository;

public class MainViewModel extends ViewModel {

    private final UpdateRepository updateRepository;

    private final MutableLiveData<UpdateResult> _updateResult = new MutableLiveData<>();
    public final LiveData<UpdateResult> updateResult = _updateResult;

    public MainViewModel(UpdateRepository updateRepository) {
        this.updateRepository = updateRepository;
    }

    public void checkUpdate() {
        updateRepository.checkUpdate(_updateResult::postValue);
    }

    public UpdateResult getCachedUpdateResult() {
        return updateRepository.getCachedResult();
    }
}
