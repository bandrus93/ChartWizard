package com.example.chartwizard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class WatchListAdapter extends RecyclerView.Adapter<WatchListAdapter.WatchListViewHolder> {
    private ArrayList<WatchListItem> mWatchList;
    private OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onShowOptionsClick(int position, ImageView show, FloatingActionButton edit, FloatingActionButton delete, FloatingActionButton oLong, FloatingActionButton oShort);
        void onEditAssetClick(int position);
        void onDeleteAssetClick(int position);
        void onOpenLongClick(int position);
        void onOpenShortClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public OnItemClickListener getOnItemClickListener() {
        return mListener;
    }

    public static class WatchListViewHolder extends RecyclerView.ViewHolder {
        public TextView mAssetSymbol;
        public TextView mAssetPrice;
        public ImageView mImageView;
        public ImageView mShowOptions;
        public FloatingActionButton mEditAsset;
        public FloatingActionButton mDeleteAsset;
        public FloatingActionButton mOpenLong;
        public FloatingActionButton mOpenShort;

        public WatchListViewHolder(@NonNull View itemView, final OnItemClickListener listener) {
            super(itemView);
            mAssetSymbol = itemView.findViewById(R.id.assetSymbol);
            mAssetPrice = itemView.findViewById(R.id.assetPrice);
            mImageView = itemView.findViewById(R.id.imageView);
            mShowOptions = itemView.findViewById(R.id.showOptions);
            mEditAsset = itemView.findViewById(R.id.editAsset);
            mDeleteAsset = itemView.findViewById(R.id.deleteAsset);
            mOpenLong = itemView.findViewById(R.id.openLong);
            mOpenShort = itemView.findViewById(R.id.openShort);

            mShowOptions.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        ImageView show = mShowOptions;
                        FloatingActionButton edit = mEditAsset;
                        FloatingActionButton delete = mDeleteAsset;
                        FloatingActionButton oLong = mOpenLong;
                        FloatingActionButton oShort = mOpenShort;
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onShowOptionsClick(position, show, edit, delete, oLong, oShort);
                        }
                    }
                }
            });

            mEditAsset.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onEditAssetClick(position);
                        }
                    }
                }
            });

            mDeleteAsset.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onDeleteAssetClick(position);
                        }
                    }
                }
            });

            mOpenLong.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onOpenLongClick(position);
                        }
                    }
                }
            });

            mOpenShort.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onOpenShortClick(position);
                        }
                    }
                }
            });
        }
    }

    public WatchListAdapter(ArrayList<WatchListItem> watchListArray) {
        mWatchList = watchListArray;
    }

    @NonNull
    @Override
    public WatchListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
        return new WatchListViewHolder(v, mListener);
    }

    @Override
    public void onBindViewHolder(@NonNull WatchListViewHolder holder, int position) {
        OnItemClickListener listener = getOnItemClickListener();
        WatchListItem currentItem = mWatchList.get(position);
        holder.mAssetSymbol.setText(currentItem.getAssetSymbol());
        holder.mAssetPrice.setText(currentItem.getAssetPrice());
        holder.mImageView.setImageResource(currentItem.getImageResource());
        holder.mShowOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    ImageView show = holder.mShowOptions;
                    FloatingActionButton edit = holder.mEditAsset;
                    FloatingActionButton delete = holder.mDeleteAsset;
                    FloatingActionButton oLong = holder.mOpenLong;
                    FloatingActionButton oShort = holder.mOpenShort;
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onShowOptionsClick(position, show, edit, delete, oLong, oShort);
                    }
                }
            }
        });
        holder.mEditAsset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onEditAssetClick(position);
                    }
                }
            }
        });
        holder.mDeleteAsset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onDeleteAssetClick(position);
                    }
                }
            }
        });
        holder.mOpenLong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onOpenLongClick(position);
                    }
                }
            }
        });
        holder.mOpenShort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onOpenShortClick(position);
                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mWatchList.size();
    }
}
