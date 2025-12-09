package se.whitered.eterna.plugins.metsgenerator.exceptions;

import org.roda.core.data.v2.jobs.PluginState;
import org.roda.core.data.v2.jobs.Report;
import org.roda.core.plugins.PluginException;

public class MetsGeneratorException extends PluginException  {
    public MetsGeneratorException(final String message) {
        super(message);
    }

    public MetsGeneratorException(final Throwable throwable) {
        super(throwable);
    }

    public void warn(final Report reportItem) {
        reportItem.setPluginState(PluginState.PARTIAL_SUCCESS).addPluginDetails(this.getMessage() + "\n");
    }
}
