package org.smartgresiter.wcaro.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ProgressBar;

import org.apache.commons.lang3.StringUtils;
import org.smartgresiter.wcaro.R;
import org.smartgresiter.wcaro.adapter.MemberAdapter;
import org.smartgresiter.wcaro.contract.FamilyChangeContract;
import org.smartgresiter.wcaro.domain.FamilyMember;
import org.smartgresiter.wcaro.listener.FloatingMenuListener;
import org.smartgresiter.wcaro.presenter.FamilyChangePresenter;
import org.smartgresiter.wcaro.util.Constants;

import java.util.ArrayList;
import java.util.List;

public class FamilyProfileChangeHead extends Fragment implements View.OnClickListener, FamilyChangeContract.View {

    protected static final String FAMILY_ID = "FAMILY_ID";
    protected String familyID;

    public FamilyProfileChangeHead() {
        // Required empty public constructor
    }

    protected MemberAdapter memberAdapter;
    RecyclerView recyclerView;
    FamilyChangeContract.Presenter presenter;
    List<FamilyMember> members;
    ProgressBar progressBar;

    public static FamilyProfileChangeHead newInstance(String familyID) {
        FamilyProfileChangeHead fragment = new FamilyProfileChangeHead();
        Bundle args = new Bundle();
        args.putString(FAMILY_ID, familyID);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            familyID = getArguments().getString(FAMILY_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_family_profile_change_head, container, false);
        prepareViews(root);
        members = new ArrayList<>();
        presenter = new FamilyChangePresenter(this, familyID);
        presenter.getAdultMembersExcludeHOF();
        return root;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.close:
                Intent returnIntent = new Intent();
                getActivity().setResult(Activity.RESULT_CANCELED, returnIntent);
                close();
                break;
            case R.id.tvAction:
                validateSave();
                break;
        }
    }

    protected void prepareViews(View view) {
        view.findViewById(R.id.close).setOnClickListener(this);
        view.findViewById(R.id.tvAction).setOnClickListener(this);
        progressBar = view.findViewById(R.id.progressBar);
        recyclerView = view.findViewById(R.id.rvList);

        progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void refreshMembersView(List<FamilyMember> familyMembers) {
        if (familyMembers != null) {
            members.clear();
            members.addAll(familyMembers);

            if (memberAdapter == null) {
                LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
                memberAdapter = new MemberAdapter(getActivity(), members);
                recyclerView.setLayoutManager(layoutManager);
                recyclerView.setAdapter(memberAdapter);
            } else {
                memberAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void saveComplete(String familyHeadID, String careGiverID) {
        progressBar.setVisibility(View.INVISIBLE);
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

        Intent returnIntent = new Intent();
        if (StringUtils.isNotBlank(familyHeadID)) {
            returnIntent.putExtra(org.smartregister.family.util.Constants.INTENT_KEY.FAMILY_HEAD, familyHeadID);
            FloatingMenuListener.getInstance(getActivity(), familyID).setFamilyHead(familyHeadID);
        }
        if (StringUtils.isNotBlank(careGiverID)) {
            returnIntent.putExtra(org.smartregister.family.util.Constants.INTENT_KEY.PRIMARY_CAREGIVER, careGiverID);
            FloatingMenuListener.getInstance(getActivity(), familyID).setPrimaryCareGiver(careGiverID);
        }
        getActivity().setResult(Activity.RESULT_OK, returnIntent);
        close();
    }

    @Override
    public void updateFamilyMember(Pair<String, FamilyMember> familyMember) {
        showProgressDialog(getString(R.string.status_saving));
        presenter.saveFamilyMember(getActivity(), familyMember);
    }

    @Override
    public void showProgressDialog(String title) {
        progressBar.setVisibility(View.VISIBLE);
        getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    @Override
    public void close() {
        getActivity().finish();
    }

    protected void validateSave() {
        Boolean valid = memberAdapter.validateSave();
        if (valid) {
            FamilyMember res = memberAdapter.getSelectedResults();
            if (res != null)
                updateFamilyMember(Pair.create(Constants.PROFILE_CHANGE_ACTION.HEAD_OF_FAMILY, res));
        }
    }
}
