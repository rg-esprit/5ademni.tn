<?php

namespace App\Form;

use App\Entity\JobApplication;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\FileType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints\File;
use Symfony\Component\Validator\Constraints\NotBlank;

class JobApplicationType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $application = $options['data'] ?? null;
        $isEdit = $application && $application->getId() && $application->getCvPath();

        $builder
            ->add('title', TextType::class, [
                'constraints' => [new NotBlank(['message' => 'Please enter a title for your application'])],
                'attr' => ['placeholder' => 'e.g. Software Engineer Application'],
            ])
            ->add('description', TextareaType::class, [
                'constraints' => [new NotBlank(['message' => 'Please provide a cover letter or description'])],
                'attr' => ['rows' => 6, 'placeholder' => 'Tell us why you are a great fit for this position...'],
            ])
            ->add('cvFile', FileType::class, [
                'label' => 'Upload CV (PDF file)',
                'mapped' => false,
                'required' => !$isEdit,
                'constraints' => array_merge(
                    $isEdit ? [] : [new NotBlank(['message' => 'Please upload your CV (PDF document)'])],
                    [
                        new File([
                            'maxSize' => '5120k', // 5MB max
                            'mimeTypes' => [
                                'application/pdf',
                                'application/x-pdf',
                            ],
                            'mimeTypesMessage' => 'Please upload a valid PDF document',
                        ])
                    ]
                ),
            ])
        ;
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => JobApplication::class,
        ]);
    }
}
