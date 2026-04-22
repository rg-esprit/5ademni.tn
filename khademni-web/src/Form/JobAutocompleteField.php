<?php

namespace App\Form;

use App\Entity\Job;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\UX\Autocomplete\Form\AsEntityAutocompleteField;
use Symfony\UX\Autocomplete\Form\BaseEntityAutocompleteType;

#[AsEntityAutocompleteField]
class JobAutocompleteField extends AbstractType
{
    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'class' => Job::class,
            'placeholder' => 'Title, company, location, or user...',
            'choice_label' => function (Job $job) {
                $user = $job->getUser();
                $userName = $user ? $user->getFirstName() . ' ' . $user->getLastName() : 'Unknown';
                return sprintf('%s at %s (%s) - Posted by %s', $job->getTitle(), $job->getCompany(), $job->getLocation(), $userName);
            },
            'searchable_fields' => ['title', 'company', 'location', 'user.firstName', 'user.lastName'],
            'tom_select_options' => [
                'create' => true,
                'createOnBlur' => true,
            ],
        ]);
    }

    public function getParent(): string
    {
        return BaseEntityAutocompleteType::class;
    }
}
