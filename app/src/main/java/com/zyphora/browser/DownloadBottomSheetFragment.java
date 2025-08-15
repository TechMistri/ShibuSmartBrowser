package com.zyphora.browser;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class DownloadBottomSheetFragment extends BottomSheetDialogFragment {

    private String videoUrl;

    public static DownloadBottomSheetFragment newInstance(String url) {
        DownloadBottomSheetFragment fragment = new DownloadBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString("url", url);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            videoUrl = getArguments().getString("url");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_download, container, false);

        TextView videoUrlTextView = view.findViewById(R.id.video_url);
        videoUrlTextView.setText(videoUrl);

        Button downloadButton = view.findViewById(R.id.btn_download);
        downloadButton.setOnClickListener(v -> {
            // Start download
            ((MainActivity) getActivity()).downloadFile(videoUrl, null, null, null);
            dismiss();
        });

        return view;
    }
}
