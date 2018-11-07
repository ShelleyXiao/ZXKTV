package tv.danmaku.ijk.media.player.misc;

import java.util.Locale;

/**
 * User: ShaudXiao
 * Date: 2018-11-01
 * Time: 16:23
 * Company: zx
 * Description:
 * FIXME
 */

public class TrackItem {
    public int mIndex;
    public ITrackInfo mTrackInfo;

    public String mInfoInline;

    public TrackItem(int index, ITrackInfo trackInfo) {
        mIndex = index;
        mTrackInfo = trackInfo;
        mInfoInline = String.format(Locale.US, "# %d: %s", mIndex, mTrackInfo.getInfoInline());
    }

    public String getInfoInline() {
        return mInfoInline;
    }
}
