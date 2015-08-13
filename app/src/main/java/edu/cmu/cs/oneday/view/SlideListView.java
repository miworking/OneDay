

package edu.cmu.cs.oneday.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Scroller;

/**
 * * @author zhangshuo
 */
public class SlideListView extends ListView {
    /**
     * listview position
     */
    private int slidePosition;
    private int downY;
    private int downX;
    private int screenWidth;
    private View itemView;
    private Scroller scroller;
    private static final int SNAP_VELOCITY = 600;
    private VelocityTracker velocityTracker;
    /**
     * �Ƿ���Ӧ������Ĭ��Ϊ����Ӧ
     */
    private boolean isSlide = false;
    /**
     * ��Ϊ���û���������С����
     */
    private int mTouchSlop;
    /**
     * �Ƴ�item��Ļص��ӿ�
     */
    private RemoveListener mRemoveListener;
    /**
     * ��ʾ�Ƿ��Ƴ�
     */
    private boolean isRemove = false;
    /**
     * ����ָʾitem������Ļ�ķ���,�����������,��һ��ö��ֵ�����
     */
    private RemoveDirection removeDirection;

    // ����ɾ�����ö��ֵ
    public enum RemoveDirection {
        RIGHT, LEFT, NONE;
    }


    public SlideListView(Context context) {
        this(context, null);
    }

    public SlideListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlideListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        screenWidth = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getWidth();
        scroller = new Scroller(context);
        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }
    public void setRemoveListener(RemoveListener removeListener) {
        this.mRemoveListener = removeListener;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                System.out.println("dispatch-->" + "down");
                addVelocityTracker(event);

                if (!scroller.isFinished()) {
                    return false;
                }
                downX = (int) event.getX();
                downY = (int) event.getY();

                slidePosition = pointToPosition(downX, downY);

                if (slidePosition == AdapterView.INVALID_POSITION) {
                    return super.dispatchTouchEvent(event);
                }

                itemView = getChildAt(slidePosition - getFirstVisiblePosition());
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                System.out.println("dispatch-->" + "move");
                if (Math.abs(getScrollVelocity()) > SNAP_VELOCITY
                        || (Math.abs(event.getX() - downX) > mTouchSlop && Math
                        .abs(event.getY() - downY) < mTouchSlop)) {
                    isSlide = true;

                }
                break;
            }
            case MotionEvent.ACTION_UP:
                recycleVelocityTracker();
                break;
        }

        return super.dispatchTouchEvent(event);
    }

    /**
     * ���һ�����getScrollX()���ص������Ե�ľ��룬������View���ԵΪԭ�㵽��ʼ�����ľ��룬�������ұ߻���Ϊ��ֵ
     */
    private void scrollRight() {
        removeDirection = RemoveDirection.RIGHT;
        final int delta = (screenWidth + itemView.getScrollX());
        scroller.startScroll(itemView.getScrollX(), 0, -delta, 0,
                Math.abs(delta));
        postInvalidate(); // ˢ��itemView
    }

    /**
     * ���󻬶��������������֪�����󻬶�Ϊ��ֵ
     */
    private void scrollLeft() {
        removeDirection = RemoveDirection.LEFT;
        final int delta = (screenWidth - itemView.getScrollX());
        // ����startScroll����������һЩ�����Ĳ���������computeScroll()�����е���scrollTo������item
        scroller.startScroll(itemView.getScrollX(), 0, delta, 0,
                Math.abs(delta));
        postInvalidate(); // ˢ��itemView
    }

    /**
     * ������ԭ����λ��
     */
    private void scrollBack() {
        removeDirection = RemoveDirection.NONE;
        scroller.startScroll(itemView.getScrollX(), 0, -itemView.getScrollX(), 0,
                Math.abs(itemView.getScrollX()));
        postInvalidate(); // ˢ��itemView
    }

    /**
     * �����ָ����itemView�ľ������ж��ǹ�������ʼλ�û�������������ҹ���
     */
    private void scrollByDistanceX() {
        // �����������ľ��������Ļ�Ķ���֮һ��������ɾ��
        if (itemView.getScrollX() >= screenWidth / 2) {
            scrollLeft();
        } else if (itemView.getScrollX() <= -screenWidth / 2) {
            scrollRight();
        } else {
            // ���ص�ԭʼλ��
            scrollBack();
        }

    }

    /**
     * ���������϶�ListView item���߼�
     */
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (isSlide && slidePosition != AdapterView.INVALID_POSITION) {
            System.out.println("touch-->" + "��ʼ");
            requestDisallowInterceptTouchEvent(true);
            addVelocityTracker(ev);
            final int action = ev.getAction();
            int x = (int) ev.getX();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    System.out.println("touch-->" + "down");
                    break;
                case MotionEvent.ACTION_MOVE:
                    System.out.println("touch-->" + "move");
                    MotionEvent cancelEvent = MotionEvent.obtain(ev);
                    cancelEvent.setAction(MotionEvent.ACTION_CANCEL |
                            (ev.getActionIndex() << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
                    onTouchEvent(cancelEvent);

                    int deltaX = downX - x;

                    // ��ָ�϶�itemView����, deltaX����0���������С��0���ҹ�
                    itemView.scrollTo(deltaX, 0);
                    // �����ָ�����ľ��룬����͸����
                    itemView.setAlpha(1f - Math.abs((float) deltaX / screenWidth));

                    return true;  //�϶���ʱ��ListView������
                case MotionEvent.ACTION_UP:
                    System.out.println("touch-->" + "up");
                    // ��ָ�뿪��ʱ��Ͳ���Ӧ���ҹ���
                    isSlide = false;
                    int velocityX = getScrollVelocity();
                    if (velocityX > SNAP_VELOCITY) {
                        scrollRight();
                    } else if (velocityX < -SNAP_VELOCITY) {
                        scrollLeft();
                    } else {
                        scrollByDistanceX();
                    }

                    recycleVelocityTracker();

                    break;
            }
        }

        //����ֱ�ӽ���ListView������onTouchEvent�¼�
        return super.onTouchEvent(ev);
    }

    @Override
    public void computeScroll() {
        // ����startScroll��ʱ��scroller.computeScrollOffset()����true��
        if (scroller.computeScrollOffset()) {
            // ��ListView item��ݵ�ǰ�Ĺ���ƫ�������й���
            itemView.scrollTo(scroller.getCurrX(), scroller.getCurrY());

            itemView.setAlpha(1f - Math.abs((float) scroller.getCurrX() / screenWidth));

            postInvalidate();

            // �������������ʱ����ûص��ӿ�
            if (scroller.isFinished() && removeDirection != RemoveDirection.NONE) {
                if (mRemoveListener == null) {
                    throw new NullPointerException("RemoveListener is null, we should called setRemoveListener()");
                }
                itemView.scrollTo(0, 0);
                itemView.setAlpha(1f);
                mRemoveListener.removeItem(removeDirection, slidePosition);
            }
        }
    }

    /**
     * ����û����ٶȸ�����
     *
     * @param event
     */
    private void addVelocityTracker(MotionEvent event) {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }

        velocityTracker.addMovement(event);
    }

    /**
     * �Ƴ��û��ٶȸ�����
     */
    private void recycleVelocityTracker() {
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }

    /**
     * ��ȡX����Ļ����ٶ�,����0���һ�������֮����
     *
     * @return
     */
    private int getScrollVelocity() {
        velocityTracker.computeCurrentVelocity(1000);
        int velocity = (int) velocityTracker.getXVelocity();
        return velocity;
    }

    /**
     * ��ListView item������Ļ���ص�����ӿ�
     * ������Ҫ�ڻص�����removeItem()���Ƴ��Item,Ȼ��ˢ��ListView
     *
     * @author xiaanming
     */
    public interface RemoveListener {
        public void removeItem(RemoveDirection direction, int position);
    }

}