package net.eq2online.permissions.plugin;

import java.util.List;

/**
 * @author Adam Mummery-Smith
 */
public interface ReplicatedPermissionsProvider
{
	public abstract boolean addMod(String modName);

	public abstract boolean removeMod(String modName);

	public abstract boolean setMinModVersion(String modName, Float modVersion);

	public abstract Float getMinModVersion(String modName);

	public abstract List<String> getMods();
}
