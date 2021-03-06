package com.important.events.mykola.kaiser.events.ui.main;

import com.arellomobile.mvp.InjectViewState;
import com.arellomobile.mvp.MvpAppCompatFragment;
import com.arellomobile.mvp.MvpPresenter;
import com.google.android.gms.common.api.GoogleApiClient;
import com.important.events.mykola.kaiser.events.MyApp;
import com.important.events.mykola.kaiser.events.database.SQLiteDatabaseEvent;
import com.important.events.mykola.kaiser.events.file.FileHelper;
import com.important.events.mykola.kaiser.events.model.Event;
import com.important.events.mykola.kaiser.events.model.User;
import com.important.events.mykola.kaiser.events.model.interface_model.IApiClient;
import com.important.events.mykola.kaiser.events.model.interface_model.IConnectCreateFragment;
import com.important.events.mykola.kaiser.events.model.interface_model.IConnectHomeFragment;
import com.important.events.mykola.kaiser.events.model.interface_model.IConnectSearchFragment;
import com.important.events.mykola.kaiser.events.model.interface_model.IReadAction;
import com.important.events.mykola.kaiser.events.model.interface_model.IUpdateAdapter;
import com.important.events.mykola.kaiser.events.model.interface_model.IUpdateRecyclerViewSearch;
import com.important.events.mykola.kaiser.events.model.interface_model.IUpdateViewCreate;
import com.important.events.mykola.kaiser.events.model.interface_model.IUpdateViewHome;
import com.important.events.mykola.kaiser.events.model.interface_model.IUserActivation;
import com.important.events.mykola.kaiser.events.ui.create.CreateFragment;
import com.important.events.mykola.kaiser.events.ui.home.HomeFragment;
import com.important.events.mykola.kaiser.events.ui.home.HomeFragmentPresenter;
import com.important.events.mykola.kaiser.events.ui.search.SearchFragment;

import java.util.ArrayList;

@InjectViewState
public class MainActivityPresenter extends MvpPresenter<IMainActivityView>
        implements IUserActivation, IUpdateAdapter, IConnectHomeFragment,
        IReadAction, IConnectCreateFragment, IConnectSearchFragment, IApiClient {
    // TODO This is bad. Don't keep links to fragments - they're views, it can lead to memory leak!
    private ArrayList<MvpAppCompatFragment> mFragments;
    private FileHelper mFileHelper;

    private boolean mCanOpenActivity;
    private boolean canClear;
    private boolean mIsUser;

    private IUpdateAdapter mIUpdateAdapter;
    private IUpdateViewCreate mIUpdateViewCreate;
    private IUpdateViewHome mIUpdateViewHome;

    private GoogleApiClient mApiClient;

    MainActivityPresenter() {
        mApiClient = null;
        canClear = false;
        mFileHelper = new FileHelper();
        mIsUser = mFileHelper.isUserBin();
        initViewPager();
        getViewState().startWork();
    }

    private void initViewPager() {
        mFragments = new ArrayList<>();


        mFragments.add(new SearchFragment(this));
        mFragments.add(new HomeFragment(this, this));
        mFragments.add(new CreateFragment(this, this));

        getViewState().addNewFragments(mFragments);
        getViewState().turnOffPage();
    }

    public boolean isUser() {
        return mIsUser;
    }

    private void initializationUser() {
        SQLiteDatabaseEvent database = MyApp.get().getDatabaseEvent();

        User user = MyApp.get().getUser();

        if (user.getMyEvents().size() == 0) {
            if (mIsUser) {
                User newUser = database.getUser(mFileHelper.readUserBin());

                user.setId(newUser.getId());
                user.setName(newUser.getName());
                user.setUri(newUser.getUri());

                user.searchMyEvent(database.getEvents(user.getId(), true));

                mIsUser = true;
            } else {
                if (database.getUser(user.getId()) != null) {
                    database.insertUser(user);
                }
            }
        }
        mFileHelper.writeUserBin(user.getId());
    }

    void setmCanOpenActivity(boolean mCanOpenActivity) {
        this.mCanOpenActivity = mCanOpenActivity;
    }

    boolean ismCanOpenActivity() {
        return mCanOpenActivity;
    }

    @Override
    public void userActivation() {
        initializationUser();
        getViewState().visibleDisplay();

        if (!mIsUser) {
            mIUpdateViewHome.updateViewHome();
            mIsUser = true;
        }
    }

    @Override
    public void updateAdapter() {
        getViewState().turnOffPage();
        mIUpdateViewHome.updateHomeAdapter();
    }

    @Override
    public void connectFragment(HomeFragmentPresenter HomePresenter) {
        mIUpdateViewHome = HomePresenter;
        mIUpdateViewHome.updateViewHome();
    }

     boolean isCanClear() {
        return canClear;
    }

     void setCanClear(boolean canClear) {
        this.canClear = canClear;
    }

    @Override
    public void signOutProfile() {
        MyApp.get().clearUser();
        mFragments.clear();
        initViewPager();
        mFileHelper.writeUserBin(FileHelper.ERROR);
        mIsUser = false;
        canClear = true;
        getViewState().signOut();
    }

    @Override
    public void startReadOrUpdate(Event event, int index, boolean can) {
        mCanOpenActivity = true;
        getViewState().startReadActivity(event, index, can);
    }

    @Override
    public void connectCreateFragment() {
        mIUpdateViewCreate = ((CreateFragment) mFragments.get(2)).mPresenter;
    }

    void changeElement(int index) {
        mIUpdateViewCreate.updateViewCreate(MyApp.get().getUser().getMyEvents().get(index), true);
    }

    void updateSearchAdapter() {
        mIUpdateAdapter.updateAdapter();
    }

    @Override
    public void connectSearchFragment() {
        IUpdateRecyclerViewSearch mIUpdateRecyclerViewSearch = ((SearchFragment) mFragments.get(0)).mPresenter;
        mIUpdateAdapter = ((SearchFragment) mFragments.get(0)).mPresenter;
        mIUpdateRecyclerViewSearch.updateRecyclerViewSearch();
    }

    void updateSubscribers() {
        try {
            ((HomeFragment) mFragments.get(1)).mPresenter.changeList(false);
        } catch (Exception e) {
            // TODO e.printStackTrace()
        }
    }

    void clearCreatePage() {
        if (mIUpdateViewCreate != null) {
            mIUpdateViewCreate.clearWhenGoNextPage();
        }
    }

    @Override
    public GoogleApiClient getAPIClient() {
        return mApiClient;
    }

    @Override
    public void setApiClient(GoogleApiClient apiClient) {
        mApiClient = apiClient;
    }
}
