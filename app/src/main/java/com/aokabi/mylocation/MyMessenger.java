package com.aokabi.mylocation;

import android.os.Messenger;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by aokabi on 2017/07/14.
 */

public class MyMessenger implements Parcelable {
    public Messenger messenger;

    public MyMessenger(Messenger msg) {
        messenger = msg;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(this.messenger, 0);
    }

    protected MyMessenger(Parcel in) {
        this.messenger = in.readParcelable(Messenger.class.getClassLoader());
    }

    public static final Parcelable.Creator<MyMessenger> CREATOR = new Parcelable.Creator<MyMessenger>() {
      public MyMessenger createFromParcel(Parcel source) {
          return new MyMessenger(source);
      }
      public MyMessenger[] newArray(int size) {
          return new MyMessenger[size];
      }
    };
}
