package org.opensrp.path.handler;

import org.opensrp.domain.form.FormSubmission;
import org.opensrp.path.service.ChildService;
import org.opensrp.service.formSubmissionHandler.FormSubmissionHandler;

/**
 * Created by muhammad.ahmed@ihsinformatics.com on 29-Oct-15.
 */
public class ChildFollowupHandler implements FormSubmissionHandler {

    private ChildService childService;

    public ChildFollowupHandler(ChildService childService) {
        this.childService = childService;
    }

    @Override
    public void handle(FormSubmission submission) {
        childService.followup(submission);
    }
}
