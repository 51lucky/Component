package com.xiaojinzi.component.application;

import android.support.annotation.MainThread;

import com.xiaojinzi.component.ComponentConstants;
import com.xiaojinzi.component.anno.support.CheckClassNameAnno;
import com.xiaojinzi.component.support.IBaseLifecycle;

/**
 * @see IApplicationLifecycle
 */
@MainThread
@CheckClassNameAnno
public interface IComponentApplication extends IApplicationLifecycle {
}
