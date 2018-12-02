package com.ehi.component.impl;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import com.ehi.component.ComponentUtil;
import com.ehi.component.error.NavigationFailException;
import com.ehi.component.error.TargetActivityNotFoundException;
import com.ehi.component.router.IComponentHostRouter;
import com.ehi.component.support.QueryParameterSupport;

import java.util.HashMap;
import java.util.Map;

/**
 * 如果名称更改了,请配置到 {@link ComponentUtil#IMPL_OUTPUT_PKG} 和 {@link ComponentUtil#UIROUTER_IMPL_CLASS_NAME} 上
 * 因为这个类是生成的子路由需要继承的类,所以这个类的包的名字的更改或者类名更改都会引起源码或者配置常量的更改
 * <p>
 * time   : 2018/07/26
 *
 * @author : xiaojinzi 30212
 */
abstract class EHiModuleRouterImpl implements IComponentHostRouter {

    /**
     * 保存映射关系的map集合
     */
    protected Map<String, Class> routerMap = new HashMap<>();

    /**
     * 记录这个Activity是否需要登录
     */
    protected Map<Class, Boolean> isNeedLoginMap = new HashMap<>();

    /**
     * 是否初始化了map,懒加载
     */
    protected boolean hasInitMap = false;

    /**
     * 上一次跳转的界面的Class
     */
    @Nullable
    private Class preTargetClass;

    /**
     * 记录上一个界面跳转的时间
     */
    private long preTargetTime;

    protected void initMap() {
        hasInitMap = true;
    }

    @Override
    public void openUri(@NonNull EHiRouterRequest routerRequest) throws Exception {
        doOpenUri(routerRequest);
    }

    /**
     * content 参数和 fragment 参数必须有一个有值的
     *
     * @param routerRequest
     * @return
     */
    private void doOpenUri(@NonNull EHiRouterRequest routerRequest) throws Exception {

        if (!hasInitMap) {
            initMap();
        }

        if (routerRequest.uri == null) {
            throw new TargetActivityNotFoundException("target Uri is null");
        }

        Class targetClass = getTargetClass(routerRequest.uri);
        // 没有找到目标界面
        if (targetClass == null) {
            throw new TargetActivityNotFoundException(routerRequest.uri.toString());
        }

        // 防止重复跳转同一个界面
        if (preTargetClass == targetClass && (System.currentTimeMillis() - preTargetTime) < 1000) { // 如果跳转的是同一个界面
            throw new NavigationFailException("target activity can't launch twice In a second");
        }

        // 保存目前跳转过去的界面
        preTargetClass = targetClass;
        preTargetTime = System.currentTimeMillis();

        if (routerRequest.context == null && routerRequest.fragment == null) {
            throw new NavigationFailException("one of the Context and Fragment must not be null,do you forget call method: \nEHiRouter.with(Context) or EHiRouter.withFragment(Fragment)");
        }

        Context context = routerRequest.context;
        if (context == null) {
            context = routerRequest.fragment.getContext();
        }

        // 如果 Context 和 Fragment 中的 Context 都是 null
        if (context == null) {
            throw new NavigationFailException("your fragment attached to Activity?");
        }

        Intent intent = new Intent(context, targetClass);
        intent.putExtras(routerRequest.bundle);
        QueryParameterSupport.put(intent, routerRequest.uri);

        // do startActivity
        doStartActivity(routerRequest, intent);

    }

    private void doStartActivity(@NonNull EHiRouterRequest routerRequest, @NonNull Intent intent) throws Exception {

        if (routerRequest.requestCode == null) { // 如果是 startActivity

            if (routerRequest.context != null) {
                routerRequest.context.startActivity(intent);
            } else if (routerRequest.fragment != null) {
                routerRequest.fragment.startActivity(intent);
            } else {
                throw new NavigationFailException("the context or fragment both are null");
            }

        } else {

            // 使用 context 跳转 startActivityForResult
            if (routerRequest.context != null) {

                Fragment rxFragment = findFragment(routerRequest.context);
                if (rxFragment != null) {
                    rxFragment.startActivityForResult(intent, routerRequest.requestCode);
                } else if (routerRequest.context instanceof Activity) {
                    ((Activity) routerRequest.context).startActivityForResult(intent, routerRequest.requestCode);
                } else {
                    throw new NavigationFailException("Context is not a Activity,so can't use 'startActivityForResult' method");
                }

            } else if (routerRequest.fragment != null) { // 使用 Fragment 跳转

                Fragment rxFragment = findFragment(routerRequest.fragment);
                if (rxFragment != null) {
                    rxFragment.startActivityForResult(intent, routerRequest.requestCode);
                } else {
                    routerRequest.fragment.startActivityForResult(intent, routerRequest.requestCode);
                }
            } else {
                throw new NavigationFailException("the context or fragment both are null");
            }

        }

    }

    @Override
    public boolean isMatchUri(@NonNull Uri uri) {

        if (!hasInitMap) {
            initMap();
        }

        return getTargetClass(uri) == null ? false : true;

    }

    @Override
    public Boolean isNeedLogin(@NonNull Uri uri) {

        if (!hasInitMap) {
            initMap();
        }

        Class<?> targetClass = getTargetClass(uri);

        return targetClass == null ? null : isNeedLoginMap.get(targetClass);

    }

    @Nullable
    private Class<?> getTargetClass(@NonNull Uri uri) {

        // "/component1/test" 不含host
        String targetPath = uri.getEncodedPath();

        if (targetPath == null || "".equals(targetPath)) {
            return null;
        }

        if (targetPath.charAt(0) != '/') {
            targetPath = "/" + targetPath;
        }

        targetPath = uri.getHost() + targetPath;

        Class targetClass = null;

        for (String key : routerMap.keySet()) {

            if (key == null || "".equals(key)) continue;

            if (key.equals(targetPath)) {
                targetClass = routerMap.get(key);
                break;
            }

        }
        return targetClass;

    }

    /**
     * 找到那个 Activity 中隐藏的一个 Fragment,如果找的到就会用这个 Fragment 拿来跳转
     *
     * @param context
     * @return
     */
    @Nullable
    private Fragment findFragment(@NonNull Context context) {
        Fragment result = null;
        if (context instanceof FragmentActivity) {
            FragmentManager ft = ((FragmentActivity) context).getSupportFragmentManager();
            result = ft.findFragmentByTag(ComponentUtil.FRAGMENT_TAG);
        }
        return result;
    }

    @Nullable
    private Fragment findFragment(@NonNull Fragment fragment) {
        Fragment result = fragment.getChildFragmentManager().findFragmentByTag(ComponentUtil.FRAGMENT_TAG);
        return result;
    }

}