package io.izzel.arclight.common.bridge.core.world.level;

import io.izzel.arclight.common.mod.util.TreeCapture;
import java.util.function.Supplier;

/** Candidate-only provider ABI; callers must retain and decide proposals outside capture. */
public interface TreeCaptureBridge {
    /** Internal active-frame identity; null outside capture. Not a commit capability. */
    Object bridge$treeCaptureIdentity();
    <T> TreeCapture.Result<T> bridge$captureTree(Supplier<T> action);
    /** Joiners must not dispatch/apply: ownedRoot=false always has an empty states list. */
    <T> TreeCapture.JoinResult<T> bridge$captureTreeOrJoin(Supplier<T> action);
}
